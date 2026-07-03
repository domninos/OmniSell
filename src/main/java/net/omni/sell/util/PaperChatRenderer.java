package net.omni.sell.util;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PaperChatRenderer implements ChatRenderer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('&').extractUrls().build();
    private static final Component PREFIX =
            MiniMessage.miniMessage().deserialize(
                    "<gray>[</gray><gradient:#00AAFF:#55FFFF>SellPortal</gradient><gray>]</gray> "
            );

    @Override
    public String parse(String message) {
        return LegacyComponentSerializer.legacySection().serialize(toComponent(message));
    }

    private static Component toComponent(String text) {
        if (text.contains("<") && text.contains(">"))
            return MINI_MESSAGE.deserialize(text);
        else
            return LEGACY.deserialize(text);
    }

    @Override
    public String color(String message) {
        return LegacyComponentSerializer.legacySection().serialize(PREFIX.append(toComponent(message)));
    }

    @Override
    public Inventory createInventory(InventoryHolder holder, int size, String title) {
        return Bukkit.createInventory(holder, size, toComponent(title));
    }

    @Override
    public void setDisplayName(ItemMeta meta, String name) {
        meta.customName(toComponent(name));
    }

    @Override
    public void setLore(ItemMeta meta, List<String> lore) {
        meta.lore(lore.stream().map(PaperChatRenderer::toComponent).toList());
    }

    @Override
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }
}
