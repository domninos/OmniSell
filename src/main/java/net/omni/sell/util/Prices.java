package net.omni.sell.util;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

public enum Prices {
    DIRT("DIRT", 1.0),
    COBBLESTONE("COBBLESTONE", 3.0),
    COAL("COAL", 5.0),
    COPPER_INGOT("COPPER_INGOT", 9.0),
    IRON_INGOT("IRON_INGOT", 15.0),
    GOLD_INGOT("GOLD_INGOT", 25.0),
    REDSTONE("REDSTONE", 40.0),
    LAPIS_LAZULI("LAPIS_LAZULI", 65.0),
    EMERALD("EMERALD", 100.0),
    DIAMOND("DIAMOND", 160.0),
    AMETHYST_SHARD("AMETHYST_SHARD", 250.0),
    QUARTZ("QUARTZ", 400.0),
    OBSIDIAN("OBSIDIAN", 650.0),
    ANCIENT_DEBRIS("ANCIENT_DEBRIS", 1000.0),
    NETHERITE_INGOT("NETHERITE_INGOT", 1600.0);

    private static final Map<Material, Prices> BY_MATERIAL = new EnumMap<>(Material.class);

    static {
        for (Prices p : values())
            BY_MATERIAL.put(Material.valueOf(p.name()), p);
    }

    public static Prices getByMaterial(Material material) {
        return BY_MATERIAL.get(material);
    }

    private final String path;
    private final double defaultPrice;
    private double cachedPrice;

    Prices(String path, double defaultPrice) {
        this.path = "prices." + path;
        this.defaultPrice = defaultPrice;
    }

    public String getPath() {
        return path;
    }

    public double getDefaultPrice() {
        return defaultPrice;
    }

    public double getPrice() {
        return cachedPrice;
    }

    public void setCachedPrice(double price) {
        this.cachedPrice = price;
    }

    public void flush() {
        this.cachedPrice = 0.0;
    }
}
