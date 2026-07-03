package net.omni.sell.commands;

import net.omni.sell.OmniSell;
import net.omni.sell.util.MessageUtil;
import net.omni.sell.util.Messages;
import net.omni.sell.util.PortalData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SellCommand implements CommandExecutor {

    private final OmniSell plugin;

    public SellCommand(OmniSell plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("omnisell.use")) {
            plugin.sendMessage(sender, Messages.NO_PERMS.toString());
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                sendHelp(sender);
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("omnisell.reload")) {
                    plugin.sendMessage(sender, Messages.NO_PERMS.toString());
                    return true;
                }

                plugin.getConfigUtil().reloadConfig();
                plugin.getMessagesManager().loadMessages();
                plugin.getPricesManager().loadPrices();
                plugin.getPortalManager().loadPortals();
                plugin.getSellItemHandler().load();

                plugin.sendMessage(sender, Messages.RELOADED.toString());
            } else if (args[0].equalsIgnoreCase("about"))
                sender.sendMessage(MessageUtil.parse(getAboutText()));
            else if (args[0].equalsIgnoreCase("give"))
                plugin.sendMessage(sender, Messages.USAGE.replace("usage", "/omnisell give (player) [amount]"));
            else if (args[0].equalsIgnoreCase("list")) {
                if (!sender.hasPermission("omnisell.list")) {
                    plugin.sendMessage(sender, Messages.NO_PERMS.toString());
                    return true;
                }

                sendPortalList(sender);
            } else if (args[0].equalsIgnoreCase("tp")) {
                if (!sender.hasPermission("omnisell.tp")) {
                    plugin.sendMessage(sender, Messages.NO_PERMS.toString());
                    return true;
                }

                plugin.sendMessage(sender, Messages.USAGE.replace("usage", "/omnisell tp (index)"));
            } else
                plugin.sendMessage(sender, Messages.UNKNOWN_COMMAND.toString());

            return true;
        } else if (args.length == 2 || args.length == 3) {
            if (args[0].equalsIgnoreCase("give"))
                return handleGive(sender, args);
            else if (args[0].equalsIgnoreCase("tp"))
                return handleTp(sender, args);
            else {
                plugin.sendMessage(sender, Messages.UNKNOWN_COMMAND.toString());
                return true;
            }
        } else
            plugin.sendMessage(sender, Messages.UNKNOWN_COMMAND.toString());

        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("omnisell.give")) {
            plugin.sendMessage(sender, Messages.NO_PERMS.toString());
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null || !target.isOnline()) {
            plugin.sendMessage(sender, Messages.PLAYER_NOT_FOUND.replace("player", args[1]));
            return true;
        }

        int amount = 1;

        if (args.length == 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                plugin.sendMessage(sender, "<red>'" + args[2] + "' is not a number.</red>");
                return true;
            }
        }

        boolean success = plugin.getSellItemHandler().give(target, amount);

        if (success)
            plugin.sendMessage(sender, Messages.GIVE_SUCCESS.replace("player", target.getName()));
        else
            plugin.sendMessage(sender, Messages.GIVE_ERROR.replace("player", target.getName()));
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("omnisell.tp")) {
            plugin.sendMessage(sender, Messages.NO_PERMS.toString());
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, Messages.ONLY_PLAYERS.toString());
            return true;
        }

        int index;
        try {
            index = Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException e) {
            plugin.sendMessage(sender, "<red>'" + args[1] + "' is not a number.</red>");
            return true;
        }

        PortalData data = plugin.getPortalManager().getPortal(index);

        if (data == null) {
            plugin.sendMessage(sender, Messages.PORTAL_NOT_FOUND.replace("index", String.valueOf(index + 1)));
            return true;
        }

        player.teleport(data.toLocation());
        plugin.sendMessage(sender, Messages.PORTAL_TP_SUCCESS.replace("index", String.valueOf(index + 1)));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        StringBuilder helpBuilder = new StringBuilder();

        helpBuilder.append("\n<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>\n");
        helpBuilder.append("  <gradient:#00AAFF:#55FFFF><bold>OmniSell</bold></gradient> <gray>\n\n");

        if (sender.hasPermission("omnisell.use")) {
            MessageUtil.append("omnisell", "Base command for OmniSell.", helpBuilder);
            MessageUtil.append("omnisell <#55FFFF>help</#55FFFF>", "Shows this help menu.", helpBuilder);

            if (sender.hasPermission("omnisell.reload"))
                MessageUtil.append("omnisell <#55FFFF>reload</#55FFFF>", "Reloads configs, messages, prices, and portals.", helpBuilder);

            MessageUtil.append("omnisell <#55FFFF>about</#55FFFF>", "Shows basic information about this plugin.", helpBuilder);

            if (sender.hasPermission("omnisell.give"))
                MessageUtil.append("omnisell give <#55FFFF>(player) [amount]</#55FFFF>", "Gives sell portal to player.", helpBuilder);

            if (sender.hasPermission("omnisell.list"))
                MessageUtil.append("omnisell <#55FFFF>list</#55FFFF>", "Lists all portals.", helpBuilder);

            if (sender.hasPermission("omnisell.tp"))
                MessageUtil.append("omnisell tp <#55FFFF>(index)</#55FFFF>", "Teleports to a portal.", helpBuilder);
        }

        helpBuilder.append("<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>");

        sender.sendMessage(MessageUtil.parse(helpBuilder.toString()));
    }

    private void sendPortalList(CommandSender sender) {
        StringBuilder listBuilder = new StringBuilder();
        List<PortalData> portals = plugin.getPortalManager().getPortals();

        listBuilder.append(Messages.PORTAL_LIST_HEADER.toString());

        if (portals.isEmpty()) {
            listBuilder.append("  ").append(Messages.PORTAL_LIST_EMPTY).append("\n");
        } else {
            for (int i = 0; i < portals.size(); i++) {
                PortalData data = portals.get(i);
                String status = data.isEnabled()
                        ? Messages.PORTAL_ENABLED.toString()
                        : Messages.PORTAL_DISABLED_STATUS.toString();
                String ownerName = Bukkit.getOfflinePlayer(data.getOwner()).getName();
                if (ownerName == null) ownerName = data.getOwner().toString().substring(0, 8) + "...";

                listBuilder.append(Messages.PORTAL_LIST_ENTRY.replace(
                        "index", String.valueOf(i + 1),
                        "world", data.getWorldName(),
                        "x", String.valueOf(data.getX()),
                        "y", String.valueOf(data.getY()),
                        "z", String.valueOf(data.getZ()),
                        "owner", ownerName,
                        "status", status
                )).append("\n");
            }
        }

        listBuilder.append(Messages.PORTAL_LIST_FOOTER);

        sender.sendMessage(MessageUtil.parse(listBuilder.toString()));
    }

    private @NotNull String getAboutText() {
        String pluginName = plugin.getDescription().getName();
        String version = plugin.getDescription().getVersion();
        String author = plugin.getDescription().getAuthors().getFirst();
        String githubUrl = "https://github.com/domninos/OmniSell";

        return "<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>\n" +
                "  <gradient:#00AAFF:#55FFFF><bold>" + pluginName + "</bold></gradient>\n\n" +
                "  <yellow>Version:</yellow> <white>" + version + "</white>\n" +
                "  <yellow>Author:</yellow> <aqua>" + author + "</aqua>\n\n" +
                "  <white>Links: </white>" +
                "<click:open_url:'" + githubUrl + "'><hover:show_text:'<gray>Click to view open-source code'><dark_purple>[GitHub]</dark_purple></hover></click>\n" +
                "<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>";
    }

    public void register() {
        PluginCommand cmd = plugin.getCommand("omnisell");

        if (cmd == null) {
            plugin.sendConsole("<red>/omnisell is not a command.</red>");
            return;
        }

        cmd.setTabCompleter((sender, command1, label, args) -> {
            List<String> subcommands = new ArrayList<>();
            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                subcommands.add("help");
                subcommands.add("about");

                if (sender.hasPermission("omnisell.reload"))
                    subcommands.add("reload");

                if (sender.hasPermission("omnisell.give"))
                    subcommands.add("give");

                if (sender.hasPermission("omnisell.list"))
                    subcommands.add("list");

                if (sender.hasPermission("omnisell.tp"))
                    subcommands.add("tp");

                StringUtil.copyPartialMatches(args[0], subcommands, completions);

                return completions;
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("give") && sender.hasPermission("omnisell.give"))
                    return null;

                if (args[0].equalsIgnoreCase("tp") && sender.hasPermission("omnisell.tp")) {
                    List<PortalData> portals = plugin.getPortalManager().getPortals();
                    for (int i = 0; i < portals.size(); i++)
                        completions.add(String.valueOf(i + 1));

                    StringUtil.copyPartialMatches(args[1], completions, completions);
                    return completions;
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("give") && sender.hasPermission("omnisell.give"))
                    completions.add("[amount]");

                return completions;
            }

            return Collections.emptyList();
        });

        cmd.setExecutor(this);
    }
}
