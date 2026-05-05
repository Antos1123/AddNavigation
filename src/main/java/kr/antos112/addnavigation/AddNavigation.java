package kr.antos112.addnavigation;

import kr.antos112.addnavigation.api.AddNavigationAPI;
import kr.antos112.addnavigation.command.NavigationCommand;
import kr.antos112.addnavigation.listener.PlayerListener;
import kr.antos112.addnavigation.model.NavigationSettings;
import kr.antos112.addnavigation.navigation.NavigationManager;
import kr.antos112.addnavigation.storage.NavigationRepository;
import kr.antos112.addnavigation.storage.SqliteNavigationRepository;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Objects;

/**
 * AddNavigation의 메인 플러그인 클래스
 */
public final class AddNavigation extends JavaPlugin {
    private static AddNavigation instance;

    private NavigationManager navigationManager;
    private NavigationRepository repository;
    private BukkitTask ticker;
    private NavigationSettings settings;

    /**
     * 동작중인 플러그인의 인스턴스를 반환합니다
     *
     * @return loaded plugin instance
     */
    public static AddNavigation getInstance() {
        return instance;
    }

    /**
     * 플러그인을 활성화하고, 설정을 불러오고, 명령/리스너 후크를 등록합니다.
     */
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        settings = NavigationSettings.from(getConfig());

        File dbFile = new File(getDataFolder(), getConfig().getString("database.sqlite.file", "navigation.db"));
        repository = new SqliteNavigationRepository(dbFile);
        repository.init();

        navigationManager = new NavigationManager(this, repository, settings);

        PluginCommand navCommand = Objects.requireNonNull(getCommand("navigation"), "navigation command missing");
        NavigationCommand executor = new NavigationCommand(this, navigationManager);
        navCommand.setExecutor(executor);
        navCommand.setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new PlayerListener(navigationManager), this);

        ticker = getServer().getScheduler().runTaskTimer(this, navigationManager::tick, 1L, 1L);
        getLogger().info("AddNavigation enabled.");
    }

    /**
     * 플러그인을 비활성화하고 모든 런타임 리소스를 해제합니다.
     */
    @Override
    public void onDisable() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        if (navigationManager != null) {
            navigationManager.shutdown();
            navigationManager = null;
        } else if (repository != null) {
            repository.close();
        }
        repository = null;
        instance = null;
    }

    /**
     * 공개 API 진입점을 반환합니다.
     *
     * @return API instance
     */
    public AddNavigationAPI getNavigationAPI() {
        return navigationManager;
    }

    /**
     * config.yml 파일을 다시 로드하고 캐시된 런타임 값을 새로 고칩니다.
     */
    public void reloadAll() {
        reloadConfig();
        settings = NavigationSettings.from(getConfig());
        if (navigationManager != null) {
            navigationManager.setSettings(settings);
            navigationManager.reload();
        }
    }

    /**
     * config.yml 파일에서 메시지 경로를 읽어 색상 코드를 적용합니다.
     *
     * @param path 메시지 섹션 내의 메시지 키
     * @return colored message
     */
    public String msg(String path) {
        return colorize(getConfig().getString("messages." + path, "&f" + path));
    }

    /**
     * '&'를 사용하여 기존 색상 코드를 변환합니다.
     *
     * @param value 원래 메세지 텍스트
     * @return colored text
     */
    public String colorize(String value) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
