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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 포인트, 세션, 경로 탐색 및 렌더링을 관리하는 중앙 런타임 서비스입니다.
 */
public final class NavigationManager implements AddNavigationAPI {
    private final AddNavigation plugin;
    private final NavigationRepository repository;
    private final Map<UUID, NavigationSession> sessions = new HashMap<>();
    private final List<UUID> sessionOrder = new ArrayList<>();
    private final ArrayDeque<UUID> pathQueue = new ArrayDeque<>();
    private final Map<String, NavigationPoint> points = new HashMap<>();
    private volatile NavigationSettings settings;
    private PathFinder pathFinder;
    private ArrowTrailRenderer renderer;
    private long tickCounter = 0L;
    private int sessionCursor = 0;

    /**
     * 새로운 manager 인스턴스를 생성합니다.
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
        loadPointCache();
    }

    /**
     * 현재 런타임 설정 스냅샷을 대체합니다.
     *
     * @param settings updated settings
     */
    public void setSettings(NavigationSettings settings) {
        this.settings = settings;
    }

    /**
     * 세션 유지 관리 단계를 한 단계 앞당깁니다.
     * <p>
     * 세션 검사와 경로 계산은 독립적으로 속도 제한이 적용되므로 큰 규모의 작업은 불가능합니다.
     * 서버는 모든 플레이어의 경로를 동일한 틱에 계산하지 않습니다.
     */
    public void tick() {
        tickCounter++;
        processPathQueue();
        processSessionBatch();
    }

    /**
     * Bukkit 위치에서 탐색 지점을 저장합니다.
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
     * 좌표에서 내비게이션 지점을 저장합니다.
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
        if (normalized.isBlank() || points.containsKey(normalized)) {
            return false;
        }

        NavigationPoint point = new NavigationPoint(normalized, worldName, x, y, z);
        boolean saved = repository.save(point);
        if (saved) {
            points.put(normalized, point);
        }
        return saved;
    }

    /**
     * 저장된 탐색 지점을 삭제하고 일치하는 모든 세션을 중지합니다.
     *
     * @param name point name
     * @return true when the point existed and was removed
     */
    @Override
    public boolean removePoint(String name) {
        String normalized = normalize(name);
        if (!points.containsKey(normalized)) {
            return false;
        }

        boolean removed = repository.delete(normalized);
        if (removed) {
            points.remove(normalized);
            for (NavigationSession session : new ArrayList<>(sessions.values())) {
                if (session.getTarget().name().equalsIgnoreCase(normalized)) {
                    stopSession(session.getPlayerId(), false);
                }
            }
        }
        return removed;
    }

    /**
     * 이름으로 저장된 포인트를 반환합니다.
     *
     * @param name point name
     * @return matching point if present
     */
    @Override
    public Optional<NavigationPoint> getPoint(String name) {
        return Optional.ofNullable(points.get(normalize(name)));
    }

    /**
     * 저장된 모든 navigation 지점을 반환합니다.
     *
     * @return all points
     */
    @Override
    public Collection<NavigationPoint> getAllPoints() {
        List<NavigationPoint> snapshot = new ArrayList<>(points.values());
        snapshot.sort(Comparator.comparing(NavigationPoint::name));
        return List.copyOf(snapshot);
    }

    /**
     * 지정된 플레이어에 대한 탐색을 시작합니다.
     *
     * @param player target player
     * @param pointName point name to navigate to
     * @return true when navigation started
     */
    @Override
    public boolean startNavigation(Player player, String pointName) {
        NavigationPoint point = points.get(normalize(pointName));
        if (point == null) return false;

        if (player.getWorld() == null || !player.getWorld().getName().equalsIgnoreCase(point.worldName())) {
            player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.world-mismatch")));
            return false;
        }

        stopSession(player.getUniqueId(), false);
        NavigationSession session = new NavigationSession(player.getUniqueId(), point);
        sessions.put(player.getUniqueId(), session);
        sessionOrder.add(player.getUniqueId());
        queuePath(session);
        player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.started")));
        return true;
    }

    /**
     * 해당 플레이어의 현재 탐색 세션을 종료합니다.
     *
     * @param player target player
     * @return true when a session existed and was stopped
     */
    @Override
    public boolean stopNavigation(Player player) {
        return stopSession(player.getUniqueId(), true);
    }

    /**
     * 메시지를 보내지 않고 현재 탐색 세션을 종료합니다.
     *
     * @param player target player
     * @return true when a session existed and was stopped
     */
    public boolean stopNavigationSilently(Player player) {
        return stopSession(player.getUniqueId(), false);
    }

    /**
     * 주어진 플레이어의 현재 세션을 반환합니다.
     *
     * @param player target player
     * @return active session if one exists
     */
    @Override
    public Optional<NavigationSession> getSession(Player player) {
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    /**
     * config.yml에 의존하는 모든 런타임 객체를 다시 로드합니다.
     */
    @Override
    public void reload() {
        this.settings = NavigationSettings.from(plugin.getConfig());
        this.pathFinder = new PathFinder(settings);
        this.renderer = new ArrowTrailRenderer(plugin, settings);
        loadPointCache();
        pathQueue.clear();

        for (NavigationSession session : sessions.values()) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            if (player != null && player.isOnline()) {
                renderer.clearAll(session, player);
            } else {
                removeIndicatorDirectly(session);
            }
            session.setCurrentPath(List.of());
            session.setPathQueued(false);
            queuePath(session);
        }
    }

    /**
     * 모든 세션을 종료하고 저장소를 닫습니다.
     */
    public void shutdown() {
        for (UUID uuid : new ArrayList<>(sessions.keySet())) {
            stopSession(uuid, false);
        }
        pathQueue.clear();
        sessionOrder.clear();
        points.clear();
        repository.close();
    }

    private void processSessionBatch() {
        if (sessionOrder.isEmpty()) {
            return;
        }

        int limit = Math.min(Math.max(1, settings.sessionChecksPerTick()), sessionOrder.size());
        int processed = 0;
        while (processed < limit && !sessionOrder.isEmpty()) {
            if (sessionCursor >= sessionOrder.size()) {
                sessionCursor = 0;
            }

            UUID uuid = sessionOrder.get(sessionCursor);
            NavigationSession session = sessions.get(uuid);
            if (session == null) {
                removeSessionOrderAt(sessionCursor);
                continue;
            }

            boolean active = maintainSession(session);
            if (active) {
                sessionCursor++;
            }
            processed++;
        }
    }

    private boolean maintainSession(NavigationSession session) {
        Player player = Bukkit.getPlayer(session.getPlayerId());
        if (player == null || !player.isOnline()) {
            stopSession(session.getPlayerId(), false);
            return false;
        }

        NavigationPoint target = session.getTarget();
        World world = Bukkit.getWorld(target.worldName());
        if (world == null || player.getWorld() == null || !player.getWorld().getName().equalsIgnoreCase(target.worldName())) {
            stopSession(player.getUniqueId(), false);
            player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.world-mismatch")));
            return false;
        }

        Location playerLocation = player.getLocation();
        Location goal = target.toLocation(world, 0.0);
        double distanceSquared = playerLocation.distanceSquared(goal);
        player.sendActionBar(plugin.msg("action-bar").replaceAll("%distance%", distanceSquared + ""));

        if (distanceSquared <= settings.autoStopDistance() * settings.autoStopDistance()) {
            complete(player);
            return false;
        }

        if (needsRepath(session, playerLocation)) {
            queuePath(session);
        }

        if (!session.getCurrentPath().isEmpty() && shouldRender(session)) {
            renderer.render(player, session, session.getCurrentPath());
            session.setLastRenderTick(tickCounter);
        }

        return true;
    }

    private boolean needsRepath(NavigationSession session, Location playerLocation) {
        if (session.isPathQueued()) {
            return false;
        }
        if (session.getCurrentPath().isEmpty()) {
            return session.getLastPathFailureTick() == 0L
                    || tickCounter - session.getLastPathFailureTick() >= settings.pathNotFoundCooldownTicks();
        }
        if (tickCounter - session.getLastPathTick() >= settings.repathIntervalTicks()) {
            return true;
        }
        return session.getLastPathOrigin() == null
                || session.getLastPathOrigin().distanceSquared(playerLocation) >= settings.repathMoveThreshold() * settings.repathMoveThreshold();
    }

    private boolean shouldRender(NavigationSession session) {
        return tickCounter - session.getLastRenderTick() >= settings.renderIntervalTicks();
    }

    private void queuePath(NavigationSession session) {
        if (session.isPathQueued()) {
            return;
        }
        session.setPathQueued(true);
        pathQueue.addLast(session.getPlayerId());
    }

    private void processPathQueue() {
        if (pathQueue.isEmpty()) {
            return;
        }

        int limit = Math.max(1, settings.maxPathfindsPerTick());
        int attempts = pathQueue.size();
        int processed = 0;
        while (processed < limit && attempts > 0 && !pathQueue.isEmpty()) {
            attempts--;
            UUID uuid = pathQueue.removeFirst();
            NavigationSession session = sessions.get(uuid);
            if (session == null) {
                continue;
            }

            session.setPathQueued(false);
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                stopSession(uuid, false);
                continue;
            }

            NavigationPoint target = session.getTarget();
            World world = Bukkit.getWorld(target.worldName());
            if (world == null || player.getWorld() == null || !player.getWorld().getName().equalsIgnoreCase(target.worldName())) {
                stopSession(uuid, false);
                player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.world-mismatch")));
                continue;
            }

            Location origin = player.getLocation();
            Location goal = target.toLocation(world, 0.0);
            List<Location> path = pathFinder.findPath(origin, goal);
            session.setCurrentPath(path);
            session.setLastPathOrigin(origin);
            session.setLastPathTick(tickCounter);
            processed++;

            if (path.isEmpty()) {
                renderer.clearAll(session, player);
                notifyPathNotFound(player, session);
                continue;
            }

            session.setLastPathFailureTick(0L);
            renderer.render(player, session, path);
            session.setLastRenderTick(tickCounter);
        }
    }

    private void notifyPathNotFound(Player player, NavigationSession session) {
        if (session.getLastPathFailureTick() != 0L
                && tickCounter - session.getLastPathFailureTick() < settings.pathNotFoundCooldownTicks()) {
            return;
        }
        session.setLastPathFailureTick(tickCounter);
        player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.path-not-found")));
    }

    private boolean stopSession(UUID uuid, boolean notify) {
        NavigationSession session = sessions.remove(uuid);
        removeSessionFromOrder(uuid);
        pathQueue.removeIf(uuid::equals);
        if (session == null) return false;

        session.setPathQueued(false);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            renderer.clearAll(session, player);
            if (notify) {
                player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.stopped")));
            }
        } else {
            removeIndicatorDirectly(session);
        }
        return true;
    }

    private void complete(Player player) {
        NavigationSession session = sessions.remove(player.getUniqueId());
        removeSessionFromOrder(player.getUniqueId());
        pathQueue.removeIf(player.getUniqueId()::equals);
        if (session != null) {
            session.setPathQueued(false);
            renderer.clearAll(session, player);
        }
        player.sendMessage(prefix() + plugin.colorize(plugin.getConfig().getString("messages.completed")));
    }

    private void removeIndicatorDirectly(NavigationSession session) {
        var indicator = session.getIndicator();
        if (indicator != null && indicator.isValid()) {
            indicator.remove();
        }
        session.setIndicator(null);
    }

    private void removeSessionFromOrder(UUID uuid) {
        int index = sessionOrder.indexOf(uuid);
        if (index >= 0) {
            removeSessionOrderAt(index);
        }
    }

    private void removeSessionOrderAt(int index) {
        sessionOrder.remove(index);
        if (index < sessionCursor) {
            sessionCursor--;
        }
        if (sessionCursor < 0 || sessionCursor >= sessionOrder.size()) {
            sessionCursor = 0;
        }
    }

    private void loadPointCache() {
        points.clear();
        for (NavigationPoint point : repository.findAll()) {
            points.put(normalize(point.name()), point);
        }
    }

    private String prefix() {
        return plugin.colorize(plugin.getConfig().getString("messages.prefix", "&7[&aAddNavigation&7] &f"));
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT);
    }
}
