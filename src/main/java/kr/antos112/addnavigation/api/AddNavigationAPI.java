package kr.antos112.addnavigation.api;

import kr.antos112.addnavigation.model.NavigationPoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

/**
 * Public API for AddNavigation.
 * <p>
 * Other plugins should depend on this interface instead of internal classes.
 * All methods are intended to be called on the Bukkit main thread unless
 * the caller explicitly guarantees thread safety for the surrounding context.
 */
public interface AddNavigationAPI {

    /**
     * Registers or updates a navigation point using the supplied Bukkit location.
     *
     * @param name unique navigation point name
     * @param location destination location
     * @return true when the point was stored successfully
     */
    boolean savePoint(String name, Location location);

    /**
     * Registers or updates a navigation point using raw world coordinates.
     *
     * @param name unique navigation point name
     * @param worldName world name
     * @param x target X coordinate
     * @param y target Y coordinate
     * @param z target Z coordinate
     * @return true when the point was stored successfully
     */
    boolean savePoint(String name, String worldName, double x, double y, double z);

    /**
     * Deletes a stored navigation point.
     *
     * @param name navigation point name
     * @return true when the point existed and was removed
     */
    boolean removePoint(String name);

    /**
     * Looks up a navigation point by name.
     *
     * @param name navigation point name
     * @return the matching point, or empty when it does not exist
     */
    Optional<NavigationPoint> getPoint(String name);

    /**
     * Returns every stored navigation point.
     *
     * @return all saved navigation points
     */
    Collection<NavigationPoint> getAllPoints();

    /**
     * Starts navigation for a player toward the selected point.
     *
     * @param player target player
     * @param pointName stored navigation point name
     * @return true when navigation started successfully
     */
    boolean startNavigation(Player player, String pointName);

    /**
     * Stops the active navigation session of a player.
     *
     * @param player target player
     * @return true when a session existed and was stopped
     */
    boolean stopNavigation(Player player);

    /**
     * Returns the current navigation session of a player.
     *
     * @param player target player
     * @return current session if the player is navigating
     */
    Optional<NavigationSession> getSession(Player player);

    /**
     * Reloads configuration-backed runtime settings.
     */
    void reload();
}
