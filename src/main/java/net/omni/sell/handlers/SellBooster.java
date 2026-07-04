package net.omni.sell.handlers;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public record SellBooster(
        String id,
        Material material,
        Component displayName,
        List<Component> lore,
        double multiplier,
        long durationSeconds,
        long cooldownSeconds,
        int guiSlot
) {
    public ItemStack createItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(displayName);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
