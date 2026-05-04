package kr.antos112.addnavigation.navigation;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Integer block position used by the A* pathfinder.
 *
 * @param x block X
 * @param y block Y
 * @param z block Z
 */
public record BlockPos(int x, int y, int z) {

    /**
     * Converts this block position to a centered Bukkit location.
     *
     * @param world target world
     * @return location centered inside the block
     */
    public Location toLocation(World world) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }
}
