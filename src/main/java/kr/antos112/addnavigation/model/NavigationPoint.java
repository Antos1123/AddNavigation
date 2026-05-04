package kr.antos112.addnavigation.model;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Stored navigation destination.
 *
 * @param name point name
 * @param worldName world name
 * @param x X coordinate
 * @param y Y coordinate
 * @param z Z coordinate
 */
public record NavigationPoint(String name, String worldName, double x, double y, double z) {

    /**
     * Converts this point to a Bukkit location.
     *
     * @param world target world
     * @return exact block coordinate location
     */
    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    /**
     * Converts this point to a Bukkit location with a configurable height offset.
     * <p>
     * This method is used by the renderer to float arrow markers above the path.
     *
     * @param world target world
     * @param heightOffset extra vertical offset
     * @return offset location for display entities
     */
    public Location toLocation(World world, double heightOffset) {
        return new Location(world, x + 0.5, y + heightOffset, z + 0.5);
    }
}
