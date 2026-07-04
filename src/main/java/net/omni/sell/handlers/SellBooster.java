package net.omni.sell.handlers;

import net.omni.sell.chat.ChatRenderer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public record SellBooster(
        String id,
        Material material,
        String displayName,
        List<String> lore,
        double multiplier,
        long durationSeconds,
        long cooldownSeconds,
        int guiSlot
) {
    public ItemStack createItem(ChatRenderer renderer) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = displayName
                    .replace("%multiplier%", String.valueOf(multiplier))
                    .replace("%duration%", formatDuration(durationSeconds))
                    .replace("%cooldown%", formatDuration(cooldownSeconds));
            renderer.setDisplayName(meta, name);

            List<String> processedLore = new ArrayList<>();

            for (String line : lore) {
                processedLore.add(line
                        .replace("%multiplier%", String.valueOf(multiplier))
                        .replace("%duration%", formatDuration(durationSeconds))
                        .replace("%cooldown%", formatDuration(cooldownSeconds)));
            }

            renderer.setLore(meta, processedLore);

            item.setItemMeta(meta);

            processedLore.clear(); // garbage
        }
        return item;
    }

    public String formatDuration(long seconds) {
        if (seconds == -1) return "Permanent";
        if (seconds == 0) return "Instant";
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
