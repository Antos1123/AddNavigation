package kr.antos112.addnavigation.model;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable runtime settings loaded from config.yml.
 *
 * @param autoStopDistance distance in blocks where navigation stops automatically
 * @param repathIntervalTicks tick interval between path recalculations
 * @param repathMoveThreshold minimum movement distance before forcing a recalculation
 * @param maxSearchNodes hard cap for A* exploration nodes
 * @param maxStepHeight maximum step-up height that the pathfinder allows
 * @param maxDropHeight maximum drop height that the pathfinder allows
 * @param arrowCharacter character rendered as the floating arrow marker
 * @param arrowColor marker color in hex format
 * @param heightOffset vertical offset applied to the rendered marker
 * @param arrowSpacing spacing between rendered arrow markers
 * @param maxArrows maximum number of visible markers
 * @param transparentBackground whether marker background should be transparent
 * @param shadowed whether the marker text should cast shadow
 */
public record NavigationSettings(
        double autoStopDistance,
        int repathIntervalTicks,
        double repathMoveThreshold,
        int maxSearchNodes,
        int maxStepHeight,
        int maxDropHeight,
        String arrowCharacter,
        String arrowColor,
        double heightOffset,
        double arrowSpacing,
        int maxArrows,
        boolean transparentBackground,
        boolean shadowed
) {
    /**
     * Builds a settings snapshot from the plugin configuration.
     *
     * @param config source configuration
     * @return loaded settings
     */
    public static NavigationSettings from(FileConfiguration config) {
        return new NavigationSettings(
                config.getDouble("navigation.auto-stop-distance", 5.0),
                config.getInt("navigation.repath-interval-ticks", 10),
                config.getDouble("navigation.repath-move-threshold", 1.0),
                config.getInt("navigation.max-search-nodes", 12000),
                config.getInt("navigation.max-step-height", 1),
                config.getInt("navigation.max-drop-height", 3),
                config.getString("display.arrow-character", "➤"),
                config.getString("display.color", "#7CFF4A"),
                config.getDouble("display.height-offset", 1.35),
                config.getDouble("display.arrow-spacing", 1.75),
                config.getInt("display.max-arrows", 32),
                config.getBoolean("display.transparent-background", true),
                config.getBoolean("display.shadowed", false)
        );
    }

    /**
     * Returns the alpha channel used for rendered marker text.
     *
     * @return full opacity byte value
     */
    public byte textOpacity() {
        return (byte) 0xFF;
    }
}
