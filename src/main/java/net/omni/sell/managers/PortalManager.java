package net.omni.sell.managers;

import net.omni.sell.OmniSell;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalManager {

    private final OmniSell plugin;

    private final Map<String, UUID> portalCache = new HashMap<>();

    public PortalManager(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void loadPortals() {
        flush();

        ConfigurationSection portals = plugin.getConfig().getConfigurationSection("portals");
    }

    public void flush() {
        portalCache.clear();
    }
}
