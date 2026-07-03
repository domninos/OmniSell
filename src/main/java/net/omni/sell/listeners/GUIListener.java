package net.omni.sell.listeners;

import net.omni.sell.OmniSell;
import net.omni.sell.handlers.SellPortal;
import net.omni.sell.handlers.SellPortalHolder;
import net.omni.sell.util.InventoryType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class GUIListener implements Listener {

    private final OmniSell plugin;

    public GUIListener(OmniSell plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();

        if (!(top.getHolder() instanceof SellPortalHolder(SellPortal portal, InventoryType type)))
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        boolean isTop = slot < top.getSize();

        switch (type) {
            case MAIN -> handleMainClick(player, portal, slot, isTop, event);
            case WHITELIST -> handleFilterClick(portal, InventoryType.WHITELIST, slot, isTop, event, view);
            case BLACKLIST -> handleFilterClick(portal, InventoryType.BLACKLIST, slot, isTop, event, view);
        }
    }

    private void handleMainClick(Player player, SellPortal portal, int slot, boolean isTop, InventoryClickEvent event) {
        if (!isTop) {
            event.setCancelled(false);
            portal.markDirty();
            return;
        }

        if (portal.isButtonSlot(slot)) {
            if (portal.isBackButtonSlot(slot)) {
                player.closeInventory();
                return;
            }

            if (portal.isWhitelistSlot(slot)) {
                player.closeInventory();
                player.openInventory(portal.buildWhitelistGUI());
            } else if (portal.isBlacklistSlot(slot)) {
                player.closeInventory();
                player.openInventory(portal.buildBlacklistGUI());
            }
            return;
        }

        event.setCancelled(false);
        portal.markDirty();
    }

    private void handleFilterClick(SellPortal portal, InventoryType type,
                                   int slot, boolean isTop, InventoryClickEvent event, InventoryView view) {
        boolean isWhitelist = type == InventoryType.WHITELIST;
        int size = isWhitelist
                ? plugin.getConfigUtil().getWhitelistSize()
                : plugin.getConfigUtil().getBlacklistSize();
        int bottomStart = size - 9;

        if (slot == plugin.getConfigUtil().getBackSlot()) {
            portal.openMainMenu((Player) event.getWhoClicked());
            return;
        }

        if (slot >= bottomStart && slot < size)
            return;

        if (!isTop) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack cursor = event.getCursor();
                if (cursor.getType() != Material.AIR) {
                    Map<Integer, ItemStack> left = event.getWhoClicked().getInventory().addItem(cursor);
                    event.setCursor(left.isEmpty() ? null : left.get(0));
                    return;
                }

                ItemStack moved = event.getCurrentItem();
                if (moved != null && moved.getType() != Material.AIR) {
                    if (portal.isInOtherFilter(type, moved)) {
                        event.setCancelled(true);
                        return;
                    }

                    Inventory top = view.getTopInventory();
                    int emptySlot = findEmptySlot(top, size - 9);
                    if (emptySlot != -1) {
                        ItemStack clone = moved.clone();
                        clone.setAmount(1);
                        top.setItem(emptySlot, clone);
                    }
                }
                return;
            }

            ItemStack moved = event.getCurrentItem();
            if (moved != null && moved.getType() != Material.AIR && portal.isInOtherFilter(type, moved)) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(false);
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor.getType() != Material.AIR) {
            if (slot < 0) {
                event.setCancelled(false);
                return;
            }

            if (portal.isInOtherFilter(type, cursor)) {
                event.setCancelled(true);
                return;
            }

            ItemStack clone = cursor.clone();
            clone.setAmount(1);
            event.setCurrentItem(clone);
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (current != null && current.getType() != Material.AIR) {
            event.setCurrentItem(null);
            portal.markDirty();
        }
    }

    private int findEmptySlot(Inventory inv, int limit) {
        for (int i = 0; i < limit; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() == Material.AIR)
                return i;
        }
        return -1;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof SellPortalHolder(SellPortal portal, InventoryType type)))
            return;

        if (type != InventoryType.MAIN) {
            int size = type == InventoryType.WHITELIST
                    ? plugin.getConfigUtil().getWhitelistSize()
                    : plugin.getConfigUtil().getBlacklistSize();

            boolean touchesTop = event.getRawSlots().stream()
                    .anyMatch(slot -> slot >= 0 && slot < size);

            if (touchesTop)
                event.setCancelled(true);
        } else {
            portal.markDirty();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;

        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();

        if (!(top.getHolder() instanceof SellPortalHolder(SellPortal portal, InventoryType type)))
            return;

        switch (type) {
            case WHITELIST -> portal.applyWhitelistChanges(top);
            case BLACKLIST -> portal.applyBlacklistChanges(top);
            case MAIN -> {}
        }

        portal.save();
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
