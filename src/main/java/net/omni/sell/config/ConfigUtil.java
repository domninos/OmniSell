package net.omni.sell.config;

import net.omni.sell.OmniSell;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

public class ConfigUtil {
    private final OmniSell plugin;

    private List<Map<String, Object>> sellBoosterDefs;

    private Material portalItemMaterial;
    private String portalItemDisplayName;
    private List<String> portalItemLore;
    private int portalSize;

    private int guiMainSize;
    private String guiMainTitle;
    private int whitelistSlot;
    private int blacklistSlot;
    private int backSlot;
    private int pickupSlot;
    private int boosterSlot;

    private Material pickupMaterial;
    private String pickupDisplayName;
    private List<String> pickupLore;

    private int whitelistSize;
    private String whitelistTitle;

    private int blacklistSize;
    private String blacklistTitle;

    private Material fillerMaterial;
    private String fillerDisplayName;

    private Material backButtonMaterial;
    private String backButtonDisplayName;
    private List<String> backButtonLore;

    private Material boosterMaterial;
    private String boosterDisplayName;
    private List<String> boosterLore;

    private int boosterSize;
    private String boosterGuiTitle;

    private Material whitelistMaterial;
    private String whitelistDisplayName;
    private List<String> whitelistLore;

    private Material blacklistMaterial;
    private String blacklistDisplayName;
    private List<String> blacklistLore;

    public ConfigUtil(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void reloadConfig() {
        flush();

        plugin.reloadConfig();
        load();
    }

    public void flush() {
        if (sellBoosterDefs != null && !sellBoosterDefs.isEmpty()) {
            sellBoosterDefs.forEach(Map::clear);
            sellBoosterDefs.clear();
        }

        portalItemLore.clear();
        whitelistLore.clear();
        blacklistLore.clear();
        pickupLore.clear();
        backButtonLore.clear();
    }

    public void load() {
        AtomicInteger savedDefaults = new AtomicInteger();

        this.portalItemMaterial = Material.matchMaterial(getAndDefaultString("portal_item.material", "END_PORTAL_FRAME", savedDefaults::getAndAdd));
        this.portalItemDisplayName = getAndDefaultString("portal_item.display_name", "<gradient:#00AAFF:#55FFFF>Sell Portal</gradient>", savedDefaults::getAndAdd);
        this.portalItemLore = plugin.getConfig().getStringList("portal_item.lore");

        this.portalSize = Math.max(1, getAndDefaultInt("portal_size", 3, savedDefaults::getAndAdd));

        this.guiMainSize = getAndDefaultInt("gui.main.size", 27, savedDefaults::addAndGet);
        this.guiMainTitle = getAndDefaultString("gui.main.title", "<gradient:#00AAFF:#55FFFF>Sell Portal</gradient>", savedDefaults::getAndAdd);

        this.boosterSize = getAndDefaultInt("gui.boosters.size", 27, savedDefaults::getAndAdd);
        this.boosterGuiTitle = getAndDefaultString("gui.boosters.title", "<gradient:#00AAFF:#55FFFF>Sell Boosters</gradient>", savedDefaults::getAndAdd);

        this.whitelistSize = getAndDefaultInt("gui.whitelist.size", 27, savedDefaults::getAndAdd);
        this.whitelistTitle = getAndDefaultString("gui.whitelist.title", "<gradient:#00AAFF:#55FFFF>Whitelist</gradient>", savedDefaults::getAndAdd);

        this.blacklistSize = getAndDefaultInt("gui.blacklist.size", 27, savedDefaults::getAndAdd);
        this.blacklistTitle = getAndDefaultString("gui.blacklist.title", "<gradient:#00AAFF:#55FFFF>Blacklist</gradient>", savedDefaults::getAndAdd);


        this.pickupSlot = getAndDefaultInt("gui.pickup_button.slot", 16, savedDefaults::getAndAdd);
        this.pickupMaterial = Material.matchMaterial(getAndDefaultString("gui.pickup_button.material", "END_PORTAL_FRAME", savedDefaults::getAndAdd));
        this.pickupDisplayName = getAndDefaultString("gui.pickup_button.display_name", "<yellow>Pick Up Portal</yellow>", savedDefaults::getAndAdd);
        this.pickupLore = plugin.getConfig().getStringList("gui.pickup_button.lore");

        this.fillerMaterial = Material.matchMaterial(getAndDefaultString("gui.filler.material", "BLACK_STAINED_GLASS_PANE", savedDefaults::getAndAdd));
        this.fillerDisplayName = getAndDefaultString("gui.filler.display_name", " ", savedDefaults::getAndAdd);

        this.backSlot = getAndDefaultInt("gui.back_button.slot", 22, savedDefaults::getAndAdd);
        this.backButtonMaterial = Material.matchMaterial(getAndDefaultString("gui.back_button.material", "BARRIER", savedDefaults::getAndAdd));
        this.backButtonDisplayName = getAndDefaultString("gui.back_button.display_name", "<red>Back</red>", savedDefaults::getAndAdd);
        this.backButtonLore = plugin.getConfig().getStringList("gui.back_button.lore");

        this.boosterSlot = getAndDefaultInt("gui.booster_button.slot", 10, savedDefaults::getAndAdd);
        this.boosterMaterial = Material.matchMaterial(getAndDefaultString("gui.booster_button.material", "EXPERIENCE_BOTTLE", savedDefaults::getAndAdd));
        this.boosterDisplayName = getAndDefaultString("gui.booster_button.display_name", "<green>Sell Boosters</green>", savedDefaults::getAndAdd);
        this.boosterLore = plugin.getConfig().getStringList("gui.booster_button.lore");

        this.whitelistSlot = getAndDefaultInt("gui.whitelist_button.slot", 12, savedDefaults::getAndAdd);
        this.whitelistMaterial = Material.matchMaterial(getAndDefaultString("gui.whitelist_button.material", "LIME_DYE", savedDefaults::getAndAdd));
        this.whitelistDisplayName = getAndDefaultString("gui.whitelist_button.display_name", "<green>Whitelist</green>", savedDefaults::getAndAdd);
        this.whitelistLore = plugin.getConfig().getStringList("gui.whitelist_button.lore");

        this.blacklistSlot = getAndDefaultInt("gui.blacklist_button.slot", 14, savedDefaults::getAndAdd);
        this.blacklistMaterial = Material.matchMaterial(getAndDefaultString("gui.blacklist_button.material", "RED_DYE", savedDefaults::getAndAdd));
        this.blacklistDisplayName = getAndDefaultString("gui.blacklist_button.display_name", "<red>Blacklist</red>", savedDefaults::getAndAdd);
        this.blacklistLore = plugin.getConfig().getStringList("gui.blacklist_button.lore");

        if (savedDefaults.get() > 0) {
            plugin.saveConfig();

            plugin.sendConsole("<green>Successfully loaded " + savedDefaults.get() + " default configuration(s)</green>");
        }

        plugin.sendConsole("<green>Successfully loaded config.yml</green>");
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

    private int getAndDefaultInt(String path, int defaultVal, IntConsumer consumer) {
        int temp = plugin.getConfig().getInt(path);

        if (!plugin.getConfig().contains(path) || temp == 0) {
            plugin.getConfig().set(path, defaultVal);
            consumer.accept(1);
            return defaultVal;
        }

        return temp;
    }

    public Material getPortalItemMaterial() {
        return this.portalItemMaterial;
    }

    public String getPortalItemDisplayName() {
        return this.portalItemDisplayName;
    }

    public List<String> getPortalItemLore() {
        return this.portalItemLore;
    }

    public int getPortalSize() {
        return this.portalSize;
    }

    public int getGuiMainSize() {
        return this.guiMainSize;
    }

    public String getGuiMainTitle() {
        return this.guiMainTitle;
    }

    public int getWhitelistSlot() {
        return this.whitelistSlot;
    }

    public int getBlacklistSlot() {
        return this.blacklistSlot;
    }

    public int getBackSlot() {
        return this.backSlot;
    }

    public int getPickupSlot() {
        return this.pickupSlot;
    }

    public int getBoosterSlot() {
        return this.boosterSlot;
    }

    public Material getPickupMat() {
        return this.pickupMaterial;
    }

    public String getPickupDisplayName() {
        return this.pickupDisplayName;
    }

    public List<String> getPickupLore() {
        return this.pickupLore;
    }

    public int getWhitelistSize() {
        return this.whitelistSize;
    }

    public String getWhitelistTitle() {
        return this.whitelistTitle;
    }

    public int getBlacklistSize() {
        return this.blacklistSize;
    }

    public String getBlacklistTitle() {
        return this.blacklistTitle;
    }

    public Material getFillerMat() {
        return this.fillerMaterial;
    }

    public String getFillerDisplayName() {
        return this.fillerDisplayName;
    }

    public Material getBackButtonMat() {
        return this.backButtonMaterial;
    }

    public String getBackButtonDisplayName() {
        return this.backButtonDisplayName;
    }

    public List<String> getBackButtonLore() {
        return this.backButtonLore;
    }

    public Material getBoosterMat() {
        return this.boosterMaterial;
    }

    public String getBoosterDisplayName() {
        return this.boosterDisplayName;
    }

    public List<String> getBoosterLore() {
        return this.boosterLore;
    }

    public int getBoostersGuiSize() {
        return this.boosterSize;
    }

    public String getBoostersGuiTitle() {
        return this.boosterGuiTitle;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSellBoosterDefs() {
        if (sellBoosterDefs != null)
            return sellBoosterDefs;

        sellBoosterDefs = new ArrayList<>();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("sell_boosters");

        if (section == null)
            return sellBoosterDefs;

        for (String key : section.getKeys(false)) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", key);
            map.put("slot", section.getInt(key + ".slot"));
            map.put("material", section.getString(key + ".material"));
            map.put("display_name", section.getString(key + ".display_name"));
            map.put("lore", section.getStringList(key + ".lore"));
            map.put("multiplier", section.getDouble(key + ".multiplier"));
            map.put("duration", section.getLong(key + ".duration"));
            map.put("cooldown", section.getLong(key + ".cooldown"));

            ConfigurationSection costsSection = section.getConfigurationSection(key + ".costs");

            Map<String, Double> costs = new HashMap<>();

            if (costsSection != null) {
                for (String currency : costsSection.getKeys(false))
                    costs.put(currency, costsSection.getDouble(currency));
            }

            map.put("costs", costs);

            sellBoosterDefs.add(map);
        }

        return sellBoosterDefs;
    }

    public Material getWhitelistMat() {
        return this.whitelistMaterial;
    }

    public String getWhitelistDisplayName() {
        return this.whitelistDisplayName;
    }

    public List<String> getWhitelistLore() {
        return this.whitelistLore;
    }

    public Material getBlacklistMat() {
        return this.blacklistMaterial;
    }

    public String getBlacklistDisplayName() {
        return this.blacklistDisplayName;
    }

    public List<String> getBlacklistLore() {
        return this.blacklistLore;
    }
}
