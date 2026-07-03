package net.omni.sell.chat;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public interface ChatRenderer {
    String parse(String message);
    String color(String message);
    Inventory createInventory(InventoryHolder holder, int size, String title);
    void setDisplayName(ItemMeta meta, String name);
    void setLore(ItemMeta meta, List<String> lore);
    void sendMessage(CommandSender sender, String message);
}