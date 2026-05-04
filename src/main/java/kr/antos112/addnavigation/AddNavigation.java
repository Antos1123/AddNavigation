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
 * Main plugin class for AddNavigation.
 */
public final class AddNavigation extends JavaPlugin {
    private static AddNavigation instance;

    private NavigationManager navigationManager;
    private NavigationRepository repository;
    private BukkitTask ticker;
    private NavigationSettings settings;

    /**
     * Returns the active plugin instance.
     *
     * @return loaded plugin instance
     */
    public static AddNavigation getInstance() {
        return instance;
    }

    /**
     * Enables the plugin, loads settings, and registers command/listener hooks.
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
     * Disables the plugin and releases all runtime resources.
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
     * Returns the public API entry point.
     *
     * @return API instance
     */
    public AddNavigationAPI getNavigationAPI() {
        return navigationManager;
    }

    /**
     * Reloads config.yml and refreshes cached runtime values.
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
     * Reads a message path from config.yml and applies color codes.
     *
     * @param path message key inside the messages section
     * @return colored message
     */
    public String msg(String path) {
        return colorize(getConfig().getString("messages." + path, "&f" + path));
    }

    /**
     * Translates legacy color codes using '&'.
     *
     * @param value raw message text
     * @return colored text
     */
    public String colorize(String value) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
