package kr.antos112.addnavigation.navigation;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * A* 경로 탐색기에서 사용하는 정수 블록 위치입니다.
 *
 * @param x block X
 * @param y block Y
 * @param z block Z
 */
public record BlockPos(int x, int y, int z) {

    /**
     * 이 블록 위치를 Bukkit의 중앙 위치로 변환합니다.
     *
     * @param world target world
     * @return location centered inside the block
     */
    public Location toLocation(World world) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }
}
