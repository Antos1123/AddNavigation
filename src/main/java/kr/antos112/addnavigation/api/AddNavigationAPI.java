package kr.antos112.addnavigation.api;

import kr.antos112.addnavigation.model.NavigationPoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

/**
 * AddNavigation의 공개 API입니다.
 * <p>
 * 다른 플러그인들은 내부 클래스 대신 이 인터페이스에 의존해야 합니다.
 * 모든 메서드는 특별한 경우가 아니면 Bukkit 메인 스레드에서 호출되도록 설계되었습니다.
 * 호출자는 주변 컨텍스트에 대한 스레드 안전성을 명시적으로 보장합니다.
 */
public interface AddNavigationAPI {

    /**
     * 제공된 Bukkit 위치 정보를 사용하여 내비게이션 지점을 등록합니다.
     *
     * @param name unique navigation point name
     * @param location destination location
     * @return true when the point was stored successfully
     */
    boolean savePoint(String name, Location location);

    /**
     * 월드 좌표를 사용하여 내비게이션 지점을 등록합니다.
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
     * 저장된 내비게이션 지점을 삭제합니다.
     *
     * @param name navigation point name
     * @return true when the point existed and was removed
     */
    boolean removePoint(String name);

    /**
     * 이름으로 내비게이션 지점을 검색합니다.
     *
     * @param name navigation point name
     * @return the matching point, or empty when it does not exist
     */
    Optional<NavigationPoint> getPoint(String name);

    /**
     * 저장된 모든 내비게이션 지점을 반환합니다.
     *
     * @return all saved navigation points
     */
    Collection<NavigationPoint> getAllPoints();

    /**
     * 플레이어가 선택한 지점으로 이동하도록 내비게이션을 시작합니다.
     *
     * @param player target player
     * @param pointName stored navigation point name
     * @return true when navigation started successfully
     */
    boolean startNavigation(Player player, String pointName);

    /**
     * 플레이어의 활성 내비게이션 세션을 종료합니다.
     *
     * @param player target player
     * @return true when a session existed and was stopped
     */
    boolean stopNavigation(Player player);

    /**
     * 플레이어의 현재 내비게이션 세션을 반환합니다.
     *
     * @param player target player
     * @return current session if the player is navigating
     */
    Optional<NavigationSession> getSession(Player player);

    /**
     * 구성 기반 런타임 설정을 다시 로드합니다.
     */
    void reload();
}
