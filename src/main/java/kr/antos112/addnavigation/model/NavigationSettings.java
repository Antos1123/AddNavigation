package kr.antos112.addnavigation.model;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Runtime settings loaded from config.yml.
 *
 * @param autoStopDistance distance in blocks where navigation stops automatically
 * @param sessionChecksPerTick maximum active sessions maintained per server tick
 * @param maxPathfindsPerTick maximum path calculations allowed per server tick
 * @param renderIntervalTicks tick interval between indicator render updates
 * @param repathIntervalTicks tick interval between path recalculations
 * @param repathMoveThreshold minimum movement distance before forcing a recalculation
 * @param pathNotFoundCooldownTicks message cooldown after a path search fails
 * @param maxSearchNodes hard cap for A* exploration nodes
 * @param maxStepHeight maximum step-up height that the pathfinder allows
 * @param maxDropHeight maximum drop height that the pathfinder allows
 * @param startSearchRadius radius used to snap the start point onto nearby walkable terrain
 * @param goalSearchRadius radius used to find walkable goal candidates around the target
 * @param verticalSearchRange vertical range used for start and goal snapping
 * @param indicatorMaterial item material used by the ItemDisplay indicator
 * @param indicatorCustomModelData custom model data for resource-pack indicators
 * @param heightOffset vertical offset applied to the rendered indicator
 * @param indicatorDistance distance in front of the player where the indicator floats
 * @param lookAheadDistance minimum distance used to choose the next path point
 * @param indicatorScale visual scale of the ItemDisplay indicator
 * @param indicatorPitchOffset pitch offset used to lay the item flat
 * @param indicatorYawOffset yaw offset for custom item models
 * @param indicatorViewRange client-side display view range
 * @param indicatorGlowing whether the indicator entity should glow
 * @param betterHudEnabled whether BetterHud compass integration is enabled
 * @param betterHudIcon BetterHud compass custom icon id
 * @param betterHudHeightOffset vertical offset used for the destination pointer
 */
public record NavigationSettings(
        double autoStopDistance,
        int sessionChecksPerTick,
        int maxPathfindsPerTick,
        int renderIntervalTicks,
        int repathIntervalTicks,
        double repathMoveThreshold,
        int pathNotFoundCooldownTicks,
        int maxSearchNodes,
        int maxStepHeight,
        int maxDropHeight,
        int startSearchRadius,
        int goalSearchRadius,
        int verticalSearchRange,
        String indicatorMaterial,
        int indicatorCustomModelData,
        double heightOffset,
        double indicatorDistance,
        double lookAheadDistance,
        double indicatorScale,
        double indicatorPitchOffset,
        double indicatorYawOffset,
        double indicatorViewRange,
        boolean indicatorGlowing,
        boolean betterHudEnabled,
        String betterHudIcon,
        double betterHudHeightOffset
) {
    /**
     * Creates a settings snapshot from plugin configuration.
     *
     * @param config source configuration
     * @return loaded settings
     */
    public static NavigationSettings from(FileConfiguration config) {
        return new NavigationSettings(
                config.getDouble("navigation.auto-stop-distance", 5.0),
                config.getInt("navigation.session-checks-per-tick", 250),
                config.getInt("navigation.max-pathfinds-per-tick", 3),
                config.getInt("navigation.render-interval-ticks", 4),
                config.getInt("navigation.repath-interval-ticks", 80),
                config.getDouble("navigation.repath-move-threshold", 3.0),
                config.getInt("navigation.path-not-found-cooldown-ticks", 100),
                config.getInt("navigation.max-search-nodes", 200000),
                config.getInt("navigation.max-step-height", 1),
                config.getInt("navigation.max-drop-height", 3),
                config.getInt("navigation.start-search-radius", 2),
                config.getInt("navigation.goal-search-radius", 6),
                config.getInt("navigation.vertical-search-range", 4),
                config.getString("display.item", "ARROW"),
                config.getInt("display.custom-model-data", 0),
                config.getDouble("display.height-offset", 0.12),
                config.getDouble("display.distance", 2.25),
                config.getDouble("display.look-ahead-distance", 2.0),
                config.getDouble("display.scale", 0.85),
                config.getDouble("display.pitch-offset", 90.0),
                config.getDouble("display.yaw-offset", 0.0),
                config.getDouble("display.view-range", 16.0),
                config.getBoolean("display.glowing", false),
                config.getBoolean("betterhud.enabled", true),
                config.getString("betterhud.icon", "addnavigation_destination"),
                config.getDouble("betterhud.height-offset", 1.2)
        );
    }

    /**
     * Resolves the configured path indicator item material.
     *
     * @return valid item material, or ARROW when the config value is invalid
     */
    public Material resolvedIndicatorMaterial() {
        return resolveMaterial(indicatorMaterial, Material.ARROW);
    }

    private Material resolveMaterial(String materialName, Material fallback) {
        Material material = Material.matchMaterial(materialName == null ? "" : materialName);
        if (material == null || !material.isItem()) {
            return fallback;
        }
        return material;
    }
}
