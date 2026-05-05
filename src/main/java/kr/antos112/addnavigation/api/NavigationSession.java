package kr.antos112.addnavigation.api;

import kr.antos112.addnavigation.model.NavigationPoint;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;

import java.util.List;
import java.util.UUID;

/**
 * 단일 활성 navigation 세션에 대한 런타임 상태입니다.
 * <p>
 * 이 객체는 navigation 지점과 생성 방향 표시기를 추적합니다,
 * 그리고 렌더러에서 사용하는 캐시된 경로 데이터입니다.
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

    /**
     * 지정된 플레이어와 목적지에 대한 새 세션을 생성합니다.
     *
     * @param playerId player unique ID
     * @param target destination point
     */
    public NavigationSession(UUID playerId, NavigationPoint target) {
        this.playerId = playerId;
        this.target = target;
    }

    /**
     * 이 세션을 소유한 플레이어의 UUID를 반환합니다.
     *
     * @return player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * 목표 내비게이션 지점을 반환합니다.
     *
     * @return target point
     */
    public NavigationPoint getTarget() {
        return target;
    }

    /**
     * 플레이어에게 현재 표시되는 경로 표시를 반환합니다.
     *
     * @return live indicator, or null when not spawned
     */
    public ItemDisplay getIndicator() {
        return indicator;
    }

    /**
     * 플레이어에게 현재 표시되는 경로 표시를 저장합니다.
     *
     * @param indicator live indicator, or null when cleared
     */
    public void setIndicator(ItemDisplay indicator) {
        this.indicator = indicator;
    }

    /**
     * 가장 최근에 계산된 경로를 반환합니다.
     *
     * @return immutable path snapshot
     */
    public List<Location> getCurrentPath() {
        return currentPath;
    }

    /**
     * 캐시된 경로를 새 스냅샷으로 바꿉니다.
     *
     * @param currentPath new path list
     */
    public void setCurrentPath(List<Location> currentPath) {
        this.currentPath = currentPath == null ? List.of() : List.copyOf(currentPath);
        this.pathProgressIndex = 0;
    }

    /**
     * 마지막 경로 재계산 시작점으로 사용된 위치를 반환합니다.
     *
     * @return last path origin, or null if not set
     */
    public Location getLastPathOrigin() {
        return lastPathOrigin;
    }

    /**
     * 경로 재계산 거리 검사에 사용되는 원점을 업데이트합니다.
     *
     * @param lastPathOrigin origin location
     */
    public void setLastPathOrigin(Location lastPathOrigin) {
        this.lastPathOrigin = lastPathOrigin == null ? null : lastPathOrigin.clone();
    }

    /**
     * 경로가 마지막으로 재계산된 시점의 틱 값을 반환합니다.
     *
     * @return last recalculation tick
     */
    public long getLastPathTick() {
        return lastPathTick;
    }

    /**
     * 경로가 다시 계산된 시점의 틱 값을 저장합니다.
     *
     * @param lastPathTick tick counter
     */
    public void setLastPathTick(long lastPathTick) {
        this.lastPathTick = lastPathTick;
    }

    /**
     * 경로가 마지막으로 렌더링된 시점의 틱 값을 반환합니다.
     *
     * @return last render tick
     */
    public long getLastRenderTick() {
        return lastRenderTick;
    }

    /**
     * 인디케이터가 렌더링된 시점의 틱 값을 저장합니다.
     *
     * @param lastRenderTick render tick
     */
    public void setLastRenderTick(long lastRenderTick) {
        this.lastRenderTick = lastRenderTick;
    }

    /**
     * 가장 최근에 경로 오류 알림이 전송된 시점을 반환합니다.
     *
     * @return last path failure notification tick
     */
    public long getLastPathFailureTick() {
        return lastPathFailureTick;
    }

    /**
     * 경로 오류 알림이 전송된 시점을 기록합니다.
     *
     * @param lastPathFailureTick failure notification tick
     */
    public void setLastPathFailureTick(long lastPathFailureTick) {
        this.lastPathFailureTick = lastPathFailureTick;
    }

    /**
     * 방향 렌더러에서 사용되는 현재 segment index를 반환합니다.
     *
     * @return current path progress segment
     */
    public int getPathProgressIndex() {
        return pathProgressIndex;
    }

    /**
     * 방향 렌더러에서 사용되는 현재 segment index를 저장합니다.
     *
     * @param pathProgressIndex current path progress segment
     */
    public void setPathProgressIndex(int pathProgressIndex) {
        this.pathProgressIndex = Math.max(0, pathProgressIndex);
    }

    /**
     * 이 세션이 이미 대기 중인 경로 계산이 있는지 여부를 반환합니다.
     *
     * @return true when a path request is queued
     */
    public boolean isPathQueued() {
        return pathQueued;
    }

    /**
     * 이 세션이 대기 중인 경로 계산 대기열에 있는지 여부를 표시합니다.
     *
     * @param pathQueued true when queued
     */
    public void setPathQueued(boolean pathQueued) {
        this.pathQueued = pathQueued;
    }
}
