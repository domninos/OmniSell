package net.omni.sell.managers;

import net.omni.sell.OmniSell;
import net.omni.sell.handlers.SellPortal;
import net.omni.sell.listeners.PortalListener;
import net.omni.sell.messages.Messages;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PortalManager {

    private final OmniSell plugin;
    private final NamespacedKey sellPortalKey;
    private final Map<Location, SellPortal> portalCache = new LinkedHashMap<>();
    private final Map<String, Location> frameToAnchor = new java.util.concurrent.ConcurrentHashMap<>();

    public PortalManager(OmniSell plugin) {
        this.plugin = plugin;
        this.sellPortalKey = new NamespacedKey(plugin, "sell_portal");
    }

    public NamespacedKey getSellPortalKey() {
        return sellPortalKey;
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
        for (String key : portal.getFrameKeys())
            frameToAnchor.put(key, location);
    }

    public void unregisterPortal(Location location) {
        SellPortal portal = portalCache.remove(location);
        if (portal != null) {
            for (String key : portal.getFrameKeys())
                frameToAnchor.remove(key);
        }
    }

    public SellPortal getPortal(Location location) {
        return portalCache.get(location);
    }

    public boolean hasPortal(Location location) {
        return portalCache.containsKey(location);
    }

    public Location getAnchorFromFrame(String blockKey) {
        return frameToAnchor.get(blockKey);
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
                for (String key : portal.getFrameKeys())
                    frameToAnchor.remove(key);
                it.remove();
            }
        }
    }

    public boolean hasPortalForIsland(String islandUUID) {
        if (islandUUID == null || islandUUID.isEmpty()) return false;
        return portalCache.values().stream()
                .anyMatch(p -> islandUUID.equals(p.getIslandUUID()));
    }

    public void handlePickupPortal(Player player, SellPortal portal) {
        if (player.getInventory().firstEmpty() == -1) {
            plugin.sendMessage(player, Messages.PORTAL_NO_EMPTY_SPACE.toString());
            return;
        }

        Location anchor = portal.getLocation();

        PortalListener.removePortalStructure(anchor, portal.getSize());
        unregisterPortal(anchor);
        plugin.getDatabaseManager().deleteLocationSync(anchor);
        player.getInventory().addItem(plugin.getSellItemHandler().getItemStack(1));
        plugin.sendMessage(player, Messages.PORTAL_PICKED_UP.toString());
    }

    public void flush() {
        portalCache.clear();
        frameToAnchor.clear();
    }
}
