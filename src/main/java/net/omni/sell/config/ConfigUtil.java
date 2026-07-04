package net.omni.sell.config;

import net.omni.sell.OmniSell;
import org.bukkit.Material;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

public class ConfigUtil {
    private final OmniSell plugin;

    public ConfigUtil(OmniSell plugin) {
        this.plugin = plugin;
    }

    public boolean reloadConfig() {
        flush();

        plugin.reloadConfig();
        load();

        // TODO
        return true;
    }

    public void flush() {
    }

    public void load() {
        AtomicInteger savedDefaults = new AtomicInteger();

        if (savedDefaults.get() > 0) {
            plugin.saveConfig();

            plugin.sendConsole("<green>Successfully loaded " + savedDefaults.get() + " default configuration(s)</green>");
        }

        plugin.sendConsole("<green>Successfully loaded config.yml</green>");
    }

    public Material getPortalItemMaterial() {
        String name = plugin.getConfig().getString("portal_item.material", "END_PORTAL_FRAME");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.END_PORTAL_FRAME;
        }
    }

    public String getPortalItemDisplayName() {
        return plugin.getConfig().getString("portal_item.display_name",
                "<gradient:#00AAFF:#55FFFF>Sell Portal</gradient>");
    }

    public List<String> getPortalItemLore() {
        return plugin.getConfig().getStringList("portal_item.lore");
    }

    public int getPortalSize() {
        return Math.max(1, plugin.getConfig().getInt("portal_size", 3));
    }

    public int getGuiMainSize() {
        return plugin.getConfig().getInt("gui.main.size", 27);
    }

    public String getGuiMainTitle() {
        return plugin.getConfig().getString("gui.main.title",
                "<gradient:#00AAFF:#55FFFF>Sell Portal</gradient>");
    }

    public int getWhitelistSlot() {
        return plugin.getConfig().getInt("gui.main.whitelist_slot", 11);
    }

    public int getBlacklistSlot() {
        return plugin.getConfig().getInt("gui.main.blacklist_slot", 15);
    }

    public int getBackSlot() {
        return plugin.getConfig().getInt("gui.main.back_slot", 22);
    }

    public int getPickupSlot() {
        return plugin.getConfig().getInt("gui.main.pickup_slot", 13);
    }

    public Material getPickupMat() {
        String name = plugin.getConfig().getString("gui.pickup_button.material", "END_PORTAL_FRAME");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.END_PORTAL_FRAME;
        }
    }

    public String getPickupDisplayName() {
        return plugin.getConfig().getString("gui.pickup_button.display_name", "<yellow>Pick Up Portal</yellow>");
    }

    public List<String> getPickupLore() {
        return plugin.getConfig().getStringList("gui.pickup_button.lore");
    }

    public int getWhitelistSize() {
        return plugin.getConfig().getInt("gui.whitelist.size", 36);
    }

    public String getWhitelistTitle() {
        return plugin.getConfig().getString("gui.whitelist.title",
                "<gradient:#00AAFF:#55FFFF>Whitelist</gradient>");
    }

    public int getBlacklistSize() {
        return plugin.getConfig().getInt("gui.blacklist.size", 36);
    }

    public String getBlacklistTitle() {
        return plugin.getConfig().getString("gui.blacklist.title",
                "<gradient:#00AAFF:#55FFFF>Blacklist</gradient>");
    }

    public Material getFillerMat() {
        String name = plugin.getConfig().getString("gui.filler.material", "BLACK_STAINED_GLASS_PANE");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.BLACK_STAINED_GLASS_PANE;
        }
    }

    public String getFillerDisplayName() {
        return plugin.getConfig().getString("gui.filler.display_name", " ");
    }

    public Material getBackButtonMat() {
        String name = plugin.getConfig().getString("gui.back_button.material", "BARRIER");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.BARRIER;
        }
    }

    public String getBackButtonDisplayName() {
        return plugin.getConfig().getString("gui.back_button.display_name", "<red>Back</red>");
    }

    public List<String> getBackButtonLore() {
        return plugin.getConfig().getStringList("gui.back_button.lore");
    }

    public Material getWhitelistMat() {
        String name = plugin.getConfig().getString("gui.whitelist_button.material", "LIME_DYE");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.LIME_DYE;
        }
    }

    public String getWhitelistDisplayName() {
        return plugin.getConfig().getString("gui.whitelist_button.display_name", "<green>Whitelist</green>");
    }

    public List<String> getWhitelistLore() {
        return plugin.getConfig().getStringList("gui.whitelist_button.lore");
    }

    public Material getBlacklistMat() {
        String name = plugin.getConfig().getString("gui.blacklist_button.material", "RED_DYE");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.RED_DYE;
        }
    }

    public String getBlacklistDisplayName() {
        return plugin.getConfig().getString("gui.blacklist_button.display_name", "<red>Blacklist</red>");
    }

    public List<String> getBlacklistLore() {
        return plugin.getConfig().getStringList("gui.blacklist_button.lore");
    }

    private int getAndDefaultSlot(String path, int defaultVal, IntConsumer consumer) {
        int temp = plugin.getConfig().getInt(path);

        if (!plugin.getConfig().contains(path) || temp == 0) {
            plugin.getConfig().set(path, defaultVal);
            consumer.accept(1);
            return defaultVal;
        }

        return temp;
    }

    private String getAndDefaultString(String path, String defaultVal, IntConsumer consumer) {
        String temp = plugin.getConfig().getString(path);

        if (temp == null) {
            plugin.getConfig().set(path, defaultVal);
            consumer.accept(1);
            return defaultVal;
        }

        return temp;
    }
}
