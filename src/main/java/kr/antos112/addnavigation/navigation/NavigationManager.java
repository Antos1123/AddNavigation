package kr.antos112.addnavigation.navigation;

import kr.antos112.addnavigation.AddNavigation;
import kr.antos112.addnavigation.api.AddNavigationAPI;
import kr.antos112.addnavigation.api.NavigationSession;
import kr.antos112.addnavigation.model.NavigationPoint;
import kr.antos112.addnavigation.model.NavigationSettings;
import kr.antos112.addnavigation.storage.NavigationRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central runtime service that manages points, sessions, pathfinding, and rendering.
 */
public final class NavigationManager implements AddNavigationAPI {
    private final AddNavigation plugin;
    private final NavigationRepository repository;
    private final Map<UUID, NavigationSession> sessions = new ConcurrentHashMap<>();
    private volatile NavigationSettings settings;
    private PathFinder pathFinder;
    private ArrowTrailRenderer renderer;
    private long tickCounter = 0L;

    /**
     * Creates a new manager instance.
     *
     * @param plugin plugin instance
     * @param repository persistent navigation point repository
     * @param settings initial runtime settings
     */
    public NavigationManager(AddNavigation plugin, NavigationRepository repository, NavigationSettings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.settings = settings;
        this.pathFinder = new PathFinder(settings);
        this.renderer = new ArrowTrailRenderer(plugin, settings);
    }

    /**
     * Replaces the current runtime settings snapshot.
     *
     * @param settings updated settings
     */
    public void setSettings(NavigationSettings settings) {
        this.settings = settings;
    }

    /**
     * Advances session maintenance by one tick.
     * <p>
     * This recalculates paths when needed, stops navigation near the destination,
     * and removes sessions for offline players.
     */
    public void tick() {
        tickCounter++;
        for (NavigationSession session : new ArrayList<>(sessions.values())) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            if (player == null || !player.isOnline()) {
                stopSession(session.getPlayerId(), false);
                continue;
            }

            NavigationPoint target = session.getTarget();
            World world = Bukkit.getWorld(target.worldName());
            if (world == null || player.getWorld() == null || !player.getWorld().getName().equalsIgnoreCase(target.worldName())) {
                stopSession(player.getUniqueId(), false);
                player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.world-mismatch")));
                continue;
            }

            Location goal = target.toLocation(world, 0.0);
            double distanceSquared = player.getLocation().distanceSquared(goal);
            if (distanceSquared <= settings.autoStopDistance() * settings.autoStopDistance()) {
                complete(player);
                continue;
            }

            boolean needsRepath = session.getCurrentPath().isEmpty()
                    || tickCounter - session.getLastPathTick() >= settings.repathIntervalTicks()
                    || session.getLastPathOrigin() == null
                    || session.getLastPathOrigin().distanceSquared(player.getLocation()) >= settings.repathMoveThreshold() * settings.repathMoveThreshold();

            if (needsRepath) {
                List<Location> path = pathFinder.findPath(player.getLocation(), goal);
                session.setCurrentPath(path);
                session.setLastPathOrigin(player.getLocation());
                session.setLastPathTick(tickCounter);
                if (path.isEmpty()) {
                    player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.path-not-found")));
                    continue;
                }
            }

            renderer.render(player, session, session.getCurrentPath());
        }
    }

    /**
     * Saves a navigation point from a Bukkit location.
     *
     * @param name point name
     * @param location destination location
     * @return true when the point was saved
     */
    @Override
    public boolean savePoint(String name, Location location) {
        if (location.getWorld() == null) return false;
        return savePoint(name, location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    /**
     * Saves a navigation point from raw coordinates.
     *
     * @param name point name
     * @param worldName world name
     * @param x target X
     * @param y target Y
     * @param z target Z
     * @return true when the point was saved
     */
    @Override
    public boolean savePoint(String name, String worldName, double x, double y, double z) {
        String normalized = normalize(name);
        if (repository.findByName(normalized).isPresent()) {
            return false;
        }
        NavigationPoint point = new NavigationPoint(normalized, worldName, x, y, z);
        return repository.save(point);
    }

    /**
     * Removes a saved navigation point and stops any matching sessions.
     *
     * @param name point name
     * @return true when the point existed and was removed
     */
    @Override
    public boolean removePoint(String name) {
        boolean removed = repository.delete(name);
        if (removed) {
            for (NavigationSession session : new ArrayList<>(sessions.values())) {
                if (session.getTarget().name().equalsIgnoreCase(name)) {
                    stopSession(session.getPlayerId(), false);
                }
            }
        }
        return removed;
    }

    /**
     * Returns a stored point by name.
     *
     * @param name point name
     * @return matching point if present
     */
    @Override
    public Optional<NavigationPoint> getPoint(String name) {
        return repository.findByName(name);
    }

    /**
     * Returns every saved navigation point.
     *
     * @return all points
     */
    @Override
    public Collection<NavigationPoint> getAllPoints() {
        return repository.findAll();
    }

    /**
     * Starts navigation for the given player.
     *
     * @param player target player
     * @param pointName point name to navigate to
     * @return true when navigation started
     */
    @Override
    public boolean startNavigation(Player player, String pointName) {
        Optional<NavigationPoint> optionalPoint = repository.findByName(pointName);
        if (optionalPoint.isEmpty()) return false;

        NavigationPoint point = optionalPoint.get();
        if (player.getWorld() == null || !player.getWorld().getName().equalsIgnoreCase(point.worldName())) {
            player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.world-mismatch")));
            return false;
        }

        stopSession(player.getUniqueId(), false);
        NavigationSession session = new NavigationSession(player.getUniqueId(), point);
        sessions.put(player.getUniqueId(), session);
        player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.started")));
        return true;
    }

    /**
     * Stops the current navigation session of the given player.
     *
     * @param player target player
     * @return true when a session existed and was stopped
     */
    @Override
    public boolean stopNavigation(Player player) {
        return stopSession(player.getUniqueId(), true);
    }

    /**
     * Returns the current session of the given player.
     *
     * @param player target player
     * @return active session if one exists
     */
    @Override
    public Optional<NavigationSession> getSession(Player player) {
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    /**
     * Reloads all runtime objects that depend on config.yml.
     */
    @Override
    public void reload() {
        this.settings = NavigationSettings.from(plugin.getConfig());
        this.pathFinder = new PathFinder(settings);
        this.renderer = new ArrowTrailRenderer(plugin, settings);
    }

    /**
     * Stops every session and closes the repository.
     */
    public void shutdown() {
        for (UUID uuid : new ArrayList<>(sessions.keySet())) {
            stopSession(uuid, false);
        }
        repository.close();
    }

    private boolean stopSession(UUID uuid, boolean notify) {
        NavigationSession session = sessions.remove(uuid);
        if (session == null) return false;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            renderer.clearAll(session, player);
            if (notify) {
                player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.stopped")));
            }
        } else {
            for (var marker : session.getMarkers()) {
                if (marker != null && marker.isValid()) marker.remove();
            }
        }
        return true;
    }

    private void complete(Player player) {
        NavigationSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            renderer.clearAll(session, player);
        }
        player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.completed")));
    }

    private String prefix() {
        return plugin.colorize(plugin.getConfig().getString("messages.prefix", "&7[&aAddNavigation&7] &f"));
    }

    private String normalize(String input) {
        return input.toLowerCase(Locale.ROOT);
    }
}
