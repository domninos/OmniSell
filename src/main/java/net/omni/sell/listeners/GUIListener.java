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

    private record PortalContext(SellPortal portal, InventoryType type) {}

    private PortalContext detect(Inventory top, InventoryView view) {
        // Primary: pattern match on SellPortalHolder
        if (top.getHolder() instanceof SellPortalHolder(SellPortal p, InventoryType t))
            return new PortalContext(p, t);

        // Fallback 1: reference equality against known portal inventories
        for (SellPortal p : plugin.getPortalManager().getPortals()) {
            if (top == p.getMainInventory())
                return new PortalContext(p, InventoryType.MAIN);
            if (top == p.getWhitelistInventory())
                return new PortalContext(p, InventoryType.WHITELIST);
            if (top == p.getBlacklistInventory())
                return new PortalContext(p, InventoryType.BLACKLIST);
        }

        // Fallback 2: detect by title (only available with InventoryView)
        if (view != null) {
            String title = view.getTitle();
            if (title == null) return null;

            String mainTitle = plugin.getConfigUtil().getGuiMainTitle();
            String whitelistTitle = plugin.getConfigUtil().getWhitelistTitle();
            String blacklistTitle = plugin.getConfigUtil().getBlacklistTitle();

            if (title.equals(whitelistTitle)) {
                SellPortal p = findPortalByInventoryMatch(top);
                if (p != null) return new PortalContext(p, InventoryType.WHITELIST);
            } else if (title.equals(blacklistTitle)) {
                SellPortal p = findPortalByInventoryMatch(top);
                if (p != null) return new PortalContext(p, InventoryType.BLACKLIST);
            } else if (title.equals(mainTitle)) {
                SellPortal p = findPortalByInventoryMatch(top);
                if (p != null) return new PortalContext(p, InventoryType.MAIN);
            }
        }

        return null;
    }

    private PortalContext detect(Inventory top) {
        return detect(top, null);
    }

    private SellPortal findPortalByInventoryMatch(Inventory top) {
        for (SellPortal p : plugin.getPortalManager().getPortals()) {
            if (top == p.getMainInventory() || top == p.getWhitelistInventory() || top == p.getBlacklistInventory())
                return p;
        }
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();

        PortalContext ctx = detect(top, view);
        if (ctx == null) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        boolean isTop = slot < top.getSize();

        switch (ctx.type()) {
            case MAIN -> handleMainClick(player, ctx.portal(), slot, isTop, event);
            case WHITELIST -> handleFilterClick(ctx.portal(), InventoryType.WHITELIST, slot, isTop, event, view);
            case BLACKLIST -> handleFilterClick(ctx.portal(), InventoryType.BLACKLIST, slot, isTop, event, view);
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
            } else if (portal.isPickupSlot(slot)) {
                plugin.getPortalManager().handlePickupPortal(player, portal);
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
        PortalContext ctx = detect(top);
        if (ctx == null) return;

        if (ctx.type() != InventoryType.MAIN) {
            int size = ctx.type() == InventoryType.WHITELIST
                    ? plugin.getConfigUtil().getWhitelistSize()
                    : plugin.getConfigUtil().getBlacklistSize();

            boolean touchesTop = event.getRawSlots().stream()
                    .anyMatch(slot -> slot >= 0 && slot < size);

            if (touchesTop)
                event.setCancelled(true);
        } else {
            ctx.portal().markDirty();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;

        Inventory top = event.getView().getTopInventory();
        PortalContext ctx = detect(top);
        if (ctx == null) return;

        switch (ctx.type()) {
            case WHITELIST -> ctx.portal().applyWhitelistChanges(top);
            case BLACKLIST -> ctx.portal().applyBlacklistChanges(top);
            case MAIN -> {}
        }

        ctx.portal().save();
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
