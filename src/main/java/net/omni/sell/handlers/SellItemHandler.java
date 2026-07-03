package net.omni.sell.handlers;

import net.omni.sell.OmniSell;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SellItemHandler {

    private final OmniSell plugin;
    private ItemStack portalItem;

    public SellItemHandler(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.portalItem = createItemStack(1);
    }

    private ItemStack createItemStack(int amount) {
        ItemStack item = new ItemStack(plugin.getConfigUtil().getPortalItemMaterial(), amount);

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        String displayName = plugin.getConfigUtil().getPortalItemDisplayName();
        if (displayName != null && !displayName.isEmpty())
            plugin.getChatRenderer().setDisplayName(meta, displayName);

        List<String> lore = plugin.getConfigUtil().getPortalItemLore();
        if (lore != null && !lore.isEmpty())
            plugin.getChatRenderer().setLore(meta, lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.getPortalManager().getSellPortalKey(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isSellPortal(ItemStack item) {
        if (item == null || item.getType() != plugin.getConfigUtil().getPortalItemMaterial())
            return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(plugin.getPortalManager().getSellPortalKey(), PersistentDataType.BYTE);
    }

    public boolean give(Player player, int amount) {
        if (player == null)
            return false;

        ItemStack item = getItemStack(amount);

        if (item == null || item.getType().isAir())
            return false;

        player.getInventory().addItem(item).values().forEach(overflow ->
                player.getWorld().dropItemNaturally(player.getLocation(), overflow));

        return true;
    }

    public ItemStack getItemStack(int amount) {
        if (portalItem == null)
            return createItemStack(amount);

        ItemStack clone = portalItem.clone();
        clone.setAmount(amount);
        return clone;
    }

    public void flush() {
        this.portalItem = null;
    }
}
