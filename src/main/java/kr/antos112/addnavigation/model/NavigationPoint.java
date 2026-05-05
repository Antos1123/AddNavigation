package kr.antos112.addnavigation.model;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * 저장된 내비게이션 목적지.
 *
 * @param name point name
 * @param worldName world name
 * @param x X coordinate
 * @param y Y coordinate
 * @param z Z coordinate
 */
public record NavigationPoint(String name, String worldName, double x, double y, double z) {

    /**
     * 이 지점을 Bukkit 위치로 변환합니다.
     *
     * @param world target world
     * @return exact block coordinate location
     */
    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    /**
     * 이 지점을 높이 오프셋을 설정할 수 있는 Bukkit 위치로 변환합니다.
     * <p>
     * 이 메서드는 렌더러가 표시 위치를 오프셋해야 할 때 사용됩니다.
     *
     * @param world target world
     * @param heightOffset extra vertical offset
     * @return offset location for display entities
     */
    public Location toLocation(World world, double heightOffset) {
        return new Location(world, x + 0.5, y + heightOffset, z + 0.5);
    }
}
