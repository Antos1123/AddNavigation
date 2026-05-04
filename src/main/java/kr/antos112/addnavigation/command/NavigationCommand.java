package kr.antos112.addnavigation.command;

import kr.antos112.addnavigation.AddNavigation;
import kr.antos112.addnavigation.model.NavigationPoint;
import kr.antos112.addnavigation.navigation.NavigationManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Handles the /navigation command family.
 */
public final class NavigationCommand implements CommandExecutor, TabCompleter {
    private final AddNavigation plugin;
    private final NavigationManager manager;

    /**
     * Creates a command handler bound to the plugin and manager.
     *
     * @param plugin plugin instance
     * @param manager navigation manager
     */
    public NavigationCommand(AddNavigation plugin, NavigationManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private boolean canUse(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("addnavigation.admin");
    }

    /**
     * Executes the /navigation command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!canUse(sender)) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    /**
     * Adds a navigation point at the sender's current world.
     */
    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(plugin.colorize("&c사용법: /navigation add <이름> <x> <y> <z>"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return;
        }
        String name = args[1];
        try {
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]);

            boolean saved = manager.savePoint(name, player.getWorld().getName(), x, y, z);
            if (saved) sender.sendMessage(plugin.msg("saved"));
            else sender.sendMessage(plugin.msg("point-exists"));
        } catch (NumberFormatException ex) {
            sender.sendMessage(plugin.colorize("&c좌표는 숫자여야 합니다."));
        }
    }

    /**
     * Removes a saved navigation point.
     */
    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&c사용법: /navigation remove <이름>"));
            return;
        }
        boolean removed = manager.removePoint(args[1]);
        sender.sendMessage(removed ? plugin.msg("removed") : plugin.msg("point-not-found"));
    }

    /**
     * Starts navigation for a target player.
     */
    private void handleStart(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.colorize("&c사용법: /navigation start <player> <이름>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("not-found"));
            return;
        }
        if (manager.getPoint(args[2]).isEmpty()) {
            sender.sendMessage(plugin.msg("point-not-found"));
            return;
        }
        boolean started = manager.startNavigation(target, args[2]);
        if (started) {
            sender.sendMessage(plugin.msg("started"));
        }
    }

    /**
     * Stops a target player's navigation.
     */
    private void handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&c사용법: /navigation stop <player>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("not-found"));
            return;
        }
        boolean stopped = manager.stopNavigation(target);
        sender.sendMessage(stopped ? plugin.msg("stopped") : plugin.msg("not-found"));
    }

    /**
     * Reloads config.yml and runtime settings.
     */
    private void handleReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(plugin.msg("reload"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.colorize("&a/navigation add <이름> <x> <y> <z>"));
        sender.sendMessage(plugin.colorize("&a/navigation remove <이름>"));
        sender.sendMessage(plugin.colorize("&a/navigation start <player> <이름>"));
        sender.sendMessage(plugin.colorize("&a/navigation stop <player>"));
        sender.sendMessage(plugin.colorize("&a/navigation reload"));
    }

    /**
     * Provides tab completion for /navigation.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!canUse(sender)) return List.of();
        if (args.length == 1) {
            return filter(List.of("add", "remove", "start", "stop", "reload"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("start"))) {
            if (args[0].equalsIgnoreCase("remove")) {
                return filter(manager.getAllPoints().stream().map(NavigationPoint::name).collect(Collectors.toList()), args[1]);
            }
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            return filter(manager.getAllPoints().stream().map(NavigationPoint::name).collect(Collectors.toList()), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stop")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> input, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : input) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(s);
        }
        return out;
    }
}
