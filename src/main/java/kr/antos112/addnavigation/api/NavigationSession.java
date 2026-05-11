package kr.antos112.addnavigation.api;

import kr.antos112.addnavigation.model.NavigationPoint;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;

import java.util.List;
import java.util.UUID;

/**
 * Mutable runtime state for one active navigation session.
 */
public final class NavigationSession {
    private final UUID playerId;
    private final NavigationPoint target;
    private ItemDisplay indicator;
    private List<Location> currentPath = List.of();
    private Location lastPathOrigin;
    private long lastPathTick;
    private long lastRenderTick;
    private long lastPathFailureTick;
    private int pathProgressIndex;
    private boolean pathQueued;

    public NavigationSession(UUID playerId, NavigationPoint target) {
        this.playerId = playerId;
        this.target = target;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public NavigationPoint getTarget() {
        return target;
    }

    public ItemDisplay getIndicator() {
        return indicator;
    }

    public void setIndicator(ItemDisplay indicator) {
        this.indicator = indicator;
    }

    public List<Location> getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(List<Location> currentPath) {
        this.currentPath = currentPath == null ? List.of() : List.copyOf(currentPath);
        this.pathProgressIndex = 0;
    }

    public Location getLastPathOrigin() {
        return lastPathOrigin;
    }

    public void setLastPathOrigin(Location lastPathOrigin) {
        this.lastPathOrigin = lastPathOrigin == null ? null : lastPathOrigin.clone();
    }

    public long getLastPathTick() {
        return lastPathTick;
    }

    public void setLastPathTick(long lastPathTick) {
        this.lastPathTick = lastPathTick;
    }

    public long getLastRenderTick() {
        return lastRenderTick;
    }

    public void setLastRenderTick(long lastRenderTick) {
        this.lastRenderTick = lastRenderTick;
    }

    public long getLastPathFailureTick() {
        return lastPathFailureTick;
    }

    public void setLastPathFailureTick(long lastPathFailureTick) {
        this.lastPathFailureTick = lastPathFailureTick;
    }

    public int getPathProgressIndex() {
        return pathProgressIndex;
    }

    public void setPathProgressIndex(int pathProgressIndex) {
        this.pathProgressIndex = Math.max(0, pathProgressIndex);
    }

    public boolean isPathQueued() {
        return pathQueued;
    }

    public void setPathQueued(boolean pathQueued) {
        this.pathQueued = pathQueued;
    }
}
