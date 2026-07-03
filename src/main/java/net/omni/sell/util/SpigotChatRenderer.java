package net.omni.sell.util;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class SpigotChatRenderer implements ChatRenderer {

    private static final Map<String, String> TAG_TO_LEGACY = Map.ofEntries(
            Map.entry("<black>", "&0"),
            Map.entry("<dark_blue>", "&1"),
            Map.entry("<dark_green>", "&2"),
            Map.entry("<dark_aqua>", "&3"),
            Map.entry("<dark_red>", "&4"),
            Map.entry("<dark_purple>", "&5"),
            Map.entry("<gold>", "&6"),
            Map.entry("<gray>", "&7"),
            Map.entry("<dark_gray>", "&8"),
            Map.entry("<blue>", "&9"),
            Map.entry("<green>", "&a"),
            Map.entry("<aqua>", "&b"),
            Map.entry("<red>", "&c"),
            Map.entry("<light_purple>", "&d"),
            Map.entry("<yellow>", "&e"),
            Map.entry("<white>", "&f"),
            Map.entry("<bold>", "&l"),
            Map.entry("<italic>", "&o"),
            Map.entry("<underlined>", "&n"),
            Map.entry("<strikethrough>", "&m"),
            Map.entry("<reset>", "&r"),
            Map.entry("</black>", "&r"),
            Map.entry("</dark_blue>", "&r"),
            Map.entry("</dark_green>", "&r"),
            Map.entry("</dark_aqua>", "&r"),
            Map.entry("</dark_red>", "&r"),
            Map.entry("</dark_purple>", "&r"),
            Map.entry("</gold>", "&r"),
            Map.entry("</gray>", "&r"),
            Map.entry("</dark_gray>", "&r"),
            Map.entry("</blue>", "&r"),
            Map.entry("</green>", "&r"),
            Map.entry("</aqua>", "&r"),
            Map.entry("</red>", "&r"),
            Map.entry("</light_purple>", "&r"),
            Map.entry("</yellow>", "&r"),
            Map.entry("</white>", "&r"),
            Map.entry("</bold>", "&r"),
            Map.entry("</italic>", "&r"),
            Map.entry("</underlined>", "&r"),
            Map.entry("</strikethrough>", "&r")
    );

    private static String miniToLegacy(String message) {
        String result = message;

        for (Map.Entry<String, String> entry : TAG_TO_LEGACY.entrySet())
            result = result.replace(entry.getKey(), entry.getValue());

        result = result.replaceAll("<#[0-9A-Fa-f]{6}>", "");
        result = result.replaceAll("</#[0-9A-Fa-f]{6}>", "&r");
        result = result.replaceAll("<gradient:[^>]+>", "");
        result = result.replaceAll("</gradient>", "&r");
        result = result.replaceAll("<hover:[^>]+>", "");
        result = result.replaceAll("</hover>", "");
        result = result.replaceAll("<click:[^>]+>", "");
        result = result.replaceAll("</click>", "");

        return result;
    }

    @Override
    public String parse(String message) {
        return ChatColor.translateAlternateColorCodes('&', miniToLegacy(message));
    }

    @Override
    public String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', "&8[&bSellPortal&8]&r " + miniToLegacy(message));
    }

    @Override
    public Inventory createInventory(InventoryHolder holder, int size, String title) {
        return Bukkit.createInventory(holder, size, parse(title));
    }

    @Override
    public void setDisplayName(ItemMeta meta, String name) {
        meta.setDisplayName(parse(name));
    }

    @Override
    public void setLore(ItemMeta meta, List<String> lore) {
        meta.setLore(lore.stream().map(this::parse).toList());
    }

    @Override
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }
}