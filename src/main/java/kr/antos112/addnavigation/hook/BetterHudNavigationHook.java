package kr.antos112.addnavigation.hook;

import kr.antos112.addnavigation.AddNavigation;
import kr.antos112.addnavigation.api.NavigationSession;
import kr.antos112.addnavigation.model.NavigationPoint;
import kr.antos112.addnavigation.model.NavigationSettings;
import kr.antos112.addnavigation.navigation.NavigationManager;
import kr.toxicity.hud.api.BetterHud;
import kr.toxicity.hud.api.BetterHudAPI;
import kr.toxicity.hud.api.adapter.LocationWrapper;
import kr.toxicity.hud.api.adapter.WorldWrapper;
import kr.toxicity.hud.api.player.PointedLocation;
import kr.toxicity.hud.api.player.PointedLocationSource;

import java.util.List;

/**
 * Sends active AddNavigation destinations to BetterHud's compass pointer system.
 */
public final class BetterHudNavigationHook {
    private static final PointedLocationSource SOURCE = PointedLocationSource.INTERNAL;

    private final AddNavigation plugin;
    private final NavigationManager navigationManager;

    public BetterHudNavigationHook(AddNavigation plugin, NavigationManager navigationManager) {
        this.plugin = plugin;
        this.navigationManager = navigationManager;
    }

    public static boolean canLoad() {
        try {
            Class.forName("kr.toxicity.hud.api.BetterHud", false, BetterHudNavigationHook.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public void register() {
        BetterHud.getInstance().getPlayerManager().addLocationProvider(hudPlayer -> {
            NavigationSettings settings = plugin.getNavigationSettings();
            if (!settings.betterHudEnabled()) {
                return List.of();
            }

            NavigationSession session = navigationManager.getSession(hudPlayer.uuid()).orElse(null);
            if (session == null) {
                return List.of();
            }

            NavigationPoint target = session.getTarget();
            if (!target.worldName().equalsIgnoreCase(hudPlayer.world().name())) {
                return List.of();
            }

            String icon = settings.betterHudIcon();
            if (icon != null && icon.isBlank()) {
                icon = null;
            }
            return List.of(new PointedLocation(
                    SOURCE,
                    "addnavigation:" + target.name(),
                    icon,
                    new LocationWrapper(
                            new WorldWrapper(target.worldName()),
                            target.x(),
                            target.y() + settings.betterHudHeightOffset(),
                            target.z(),
                            0.0f,
                            0.0f
                    )
            ));
        });
        plugin.getLogger().info("BetterHud destination pointer hook enabled.");
    }
}
