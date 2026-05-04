package kr.antos112.addnavigation.api;

import kr.antos112.addnavigation.model.NavigationPoint;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runtime state for a single active navigation session.
 * <p>
 * This object keeps track of the target point, spawned arrow markers,
 * and cached path data used by the renderer.
 */
public final class NavigationSession {
    private final UUID playerId;
    private final NavigationPoint target;
    private final List<TextDisplay> markers = new ArrayList<>();
    private List<Location> currentPath = List.of();
    private Location lastPathOrigin;
    private long lastPathTick;

    /**
     * Creates a new session for the given player and destination.
     *
     * @param playerId player unique ID
     * @param target destination point
     */
    public NavigationSession(UUID playerId, NavigationPoint target) {
        this.playerId = playerId;
        this.target = target;
    }

    /**
     * Returns the unique ID of the player that owns this session.
     *
     * @return player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns the target navigation point.
     *
     * @return target point
     */
    public NavigationPoint getTarget() {
        return target;
    }

    /**
     * Returns the arrow marker entities currently shown to the player.
     *
     * @return live marker list
     */
    public List<TextDisplay> getMarkers() {
        return markers;
    }

    /**
     * Returns the most recently computed path.
     *
     * @return immutable path snapshot
     */
    public List<Location> getCurrentPath() {
        return currentPath;
    }

    /**
     * Replaces the cached path with a new snapshot.
     *
     * @param currentPath new path list
     */
    public void setCurrentPath(List<Location> currentPath) {
        this.currentPath = currentPath == null ? List.of() : List.copyOf(currentPath);
    }

    /**
     * Returns the location used as the last path recalculation origin.
     *
     * @return last path origin, or null if not set
     */
    public Location getLastPathOrigin() {
        return lastPathOrigin;
    }

    /**
     * Updates the origin used for path recalculation distance checks.
     *
     * @param lastPathOrigin origin location
     */
    public void setLastPathOrigin(Location lastPathOrigin) {
        this.lastPathOrigin = lastPathOrigin == null ? null : lastPathOrigin.clone();
    }

    /**
     * Returns the tick value when the path was last recalculated.
     *
     * @return last recalculation tick
     */
    public long getLastPathTick() {
        return lastPathTick;
    }

    /**
     * Stores the tick value when the path was recalculated.
     *
     * @param lastPathTick tick counter
     */
    public void setLastPathTick(long lastPathTick) {
        this.lastPathTick = lastPathTick;
    }
}
