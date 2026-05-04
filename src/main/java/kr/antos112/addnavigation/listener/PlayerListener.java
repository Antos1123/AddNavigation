package kr.antos112.addnavigation.listener;

import kr.antos112.addnavigation.navigation.NavigationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans up active navigation sessions on player disconnects or world changes.
 */
public final class PlayerListener implements Listener {
    private final NavigationManager manager;

    /**
     * Creates a listener that delegates cleanup to the navigation manager.
     *
     * @param manager navigation manager
     */
    public PlayerListener(NavigationManager manager) {
        this.manager = manager;
    }

    /**
     * Stops navigation when a player leaves the server.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.stopNavigation(event.getPlayer());
    }

    /**
     * Stops navigation when a player changes worlds.
     */
    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        manager.stopNavigation(event.getPlayer());
    }
}
