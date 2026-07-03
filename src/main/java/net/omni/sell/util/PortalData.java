package net.omni.sell.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

public class PortalData {

    private final String locationKey;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final UUID owner;
    private boolean enabled;

    public PortalData(String locationKey, String worldName, int x, int y, int z, UUID owner, boolean enabled) {
        this.locationKey = locationKey;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.owner = owner;
        this.enabled = enabled;
    }

    public static PortalData fromKey(String key, UUID owner, boolean enabled) {
        String[] parts = key.split(",");
        String world = parts[0];
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        return new PortalData(key, world, x, y, z, owner, enabled);
    }

    public Location toLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public String getLocationKey() { return locationKey; }
    public String getWorldName() { return worldName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public UUID getOwner() { return owner; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
