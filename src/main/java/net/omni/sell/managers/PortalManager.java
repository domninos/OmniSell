package net.omni.sell.managers;

import net.omni.sell.OmniSell;
import net.omni.sell.util.PortalData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class PortalManager {

    private final OmniSell plugin;
    private final Map<String, PortalData> portalCache = new LinkedHashMap<>();
    private boolean globallyEnabled = true;

    public PortalManager(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void loadPortals() {
        flush();
        plugin.getPortalsConfig().reload();

        FileConfiguration config = plugin.getPortalsConfig().getConfig();

        this.globallyEnabled = config.getBoolean("enabled", true);

        ConfigurationSection portalsSection = config.getConfigurationSection("portals");
        if (portalsSection == null) return;

        for (String key : portalsSection.getKeys(false)) {
            String ownerStr = portalsSection.getString(key + ".owner");
            boolean enabled = portalsSection.getBoolean(key + ".enabled", true);

            if (ownerStr == null) continue;

            try {
                UUID owner = UUID.fromString(ownerStr);
                PortalData data = PortalData.fromKey(key, owner, enabled);
                portalCache.put(key, data);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public boolean isGloballyEnabled() {
        return globallyEnabled;
    }

    public void setGloballyEnabled(boolean globallyEnabled) {
        this.globallyEnabled = globallyEnabled;
        plugin.getPortalsConfig().set("enabled", globallyEnabled);
    }

    public boolean isPortalEnabled(String key) {
        if (!globallyEnabled) return false;
        PortalData data = portalCache.get(key);
        return data != null && data.isEnabled();
    }

    public List<PortalData> getPortals() {
        return List.copyOf(portalCache.values());
    }

    public PortalData getPortal(int index) {
        List<PortalData> list = getPortals();
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    public PortalData getPortal(String key) {
        return portalCache.get(key);
    }

    public void togglePortal(String key) {
        PortalData data = portalCache.get(key);
        if (data == null) return;
        data.setEnabled(!data.isEnabled());
        plugin.getPortalsConfig().set("portals." + key + ".enabled", data.isEnabled());
    }

    public void save() {
        plugin.getPortalsConfig().save();
    }

    public void flush() {
        portalCache.clear();
        globallyEnabled = true;
    }
}
