package net.omni.sell.util;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;

public class ItemSerializationUtil {

    private static final boolean PAPER;

    static {
        boolean paper = false;
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            paper = true;
        } catch (ClassNotFoundException ignored) {
        }
        PAPER = paper;
    }

    public static String toBase64(List<ItemStack> items) {
        if (items == null || items.isEmpty())
            return "";

        try {
            if (PAPER)
                return paperToBase64(items);
            else
                return spigotToBase64(items);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to serialize items to base64", e);
            return "";
        }
    }

    private static String paperToBase64(List<ItemStack> items) {
        byte[] bytes = ItemStack.serializeItemsAsBytes(items.toArray(new ItemStack[0]));
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String spigotToBase64(List<ItemStack> items) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("items", items);
        String yaml = config.saveToString();
        return Base64.getEncoder().encodeToString(yaml.getBytes(StandardCharsets.UTF_8));
    }

    public static List<ItemStack> fromBase64(String base64) {
        if (base64 == null || base64.isBlank())
            return List.of();

        try {
            if (PAPER)
                return paperFromBase64(base64);
            else
                return spigotFromBase64(base64);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to deserialize items from base64", e);
            return List.of();
        }
    }

    private static List<ItemStack> paperFromBase64(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return List.of(ItemStack.deserializeItemsFromBytes(bytes));
    }

    private static List<ItemStack> spigotFromBase64(String base64) {
        byte[] data = Base64.getDecoder().decode(base64);
        YamlConfiguration config = new YamlConfiguration();

        try {
            config.loadFromString(new String(data, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }

        List<?> raw = config.getList("items");

        if (raw == null)
            return List.of();

        List<ItemStack> items = new ArrayList<>();

        for (Object obj : raw) {
            if (obj instanceof ItemStack item)
                items.add(item);
        }

        return items;
    }
}
