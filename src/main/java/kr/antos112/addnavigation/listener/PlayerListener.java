package kr.antos112.addnavigation.listener;

import kr.antos112.addnavigation.navigation.NavigationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 플레이어 연결이 끊기거나 월드가 변경될 때 활성 탐색 세션을 정리합니다.
 */
public final class PlayerListener implements Listener {
    private final NavigationManager manager;

    /**
     * 정리 작업을 내비게이션 manager에게 위임하는 리스너를 생성합니다.
     *
     * @param manager navigation manager
     */
    public PlayerListener(NavigationManager manager) {
        this.manager = manager;
    }

    /**
     * 플레이어가 서버를 떠나면 탐색을 중지합니다.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.stopNavigationSilently(event.getPlayer());
    }

    /**
     * 플레이어가 월드를 변경할 때 탐색을 중지합니다.
     */
    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        manager.stopNavigationSilently(event.getPlayer());
    }
}
