package net.omni.sell.managers;

import net.omni.sell.OmniSell;
import net.omni.sell.handlers.SellPortal;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.stream.Collectors;

public class PortalManager {

    private final OmniSell plugin;
    private final NamespacedKey sellPortalKey;
    private final NamespacedKey portalAnchorKey;
    private final Map<Location, SellPortal> portalCache = new LinkedHashMap<>();

    public PortalManager(OmniSell plugin) {
        this.plugin = plugin;
        this.sellPortalKey = new NamespacedKey(plugin, "sell_portal");
        this.portalAnchorKey = new NamespacedKey(plugin, "portal_anchor");
    }

    public NamespacedKey getSellPortalKey() {
        return sellPortalKey;
    }

    public NamespacedKey getPortalAnchorKey() {
        return portalAnchorKey;
    }

    public boolean isGloballyEnabled() {
        return plugin.getConfig().getBoolean("portals.enabled", true);
    }

    public void setGloballyEnabled(boolean enabled) {
        plugin.getConfig().set("portals.enabled", enabled);
        plugin.saveConfig();
    }

    public void registerPortal(Location location, SellPortal portal) {
        portalCache.put(location, portal);
    }

    public void unregisterPortal(Location location) {
        portalCache.remove(location);
    }

    public SellPortal getPortal(Location location) {
        return portalCache.get(location);
    }

    public boolean hasPortal(Location location) {
        return portalCache.containsKey(location);
    }

    public List<SellPortal> getPortals() {
        return List.copyOf(portalCache.values());
    }

    public List<SellPortal> getPortalsByOwner(UUID ownerUUID) {
        return portalCache.values().stream()
                .filter(p -> p.getOwnerUUID().equals(ownerUUID))
                .collect(Collectors.toList());
    }

    public void saveDirty() {
        for (SellPortal portal : portalCache.values()) {
            if (portal.isDirty())
                portal.save();
        }
    }

    public void saveAndUnload(Chunk chunk) {
        Iterator<Map.Entry<Location, SellPortal>> it = portalCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, SellPortal> entry = it.next();
            Location loc = entry.getKey();
            if (loc.getWorld().equals(chunk.getWorld())
                    && chunk.getX() == loc.getBlockX() >> 4
                    && chunk.getZ() == loc.getBlockZ() >> 4) {
                SellPortal portal = entry.getValue();
                if (portal.isDirty())
                    portal.save();
                it.remove();
            }
        }
    }

    public void flush() {
        portalCache.clear();
    }
}
