package net.omni.sell.listeners;

import net.omni.sell.OmniSell;
import net.omni.sell.handlers.SellBooster;
import net.omni.sell.handlers.SellPortal;
import net.omni.sell.handlers.SellPortalHolder;
import net.omni.sell.util.InventoryType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

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

        PortalContext ctx = detect(top, view);
        if (ctx == null) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        boolean isTop = slot < top.getSize();

        switch (ctx.type()) {
            case MAIN -> handleMainClick(player, ctx.portal(), slot, isTop, event);
            case WHITELIST -> handleFilterClick(ctx.portal(), InventoryType.WHITELIST, slot, isTop, event, view);
            case BLACKLIST -> handleFilterClick(ctx.portal(), InventoryType.BLACKLIST, slot, isTop, event, view);
            case BOOSTER -> handleBoosterClick(player, ctx.portal(), slot);
        }
    }

    private PortalContext detect(Inventory top, InventoryView view) {
        InventoryHolder holder = top.getHolder();

        if (holder instanceof SellPortalHolder(SellPortal portal, InventoryType type))
            return new PortalContext(portal, type);

        for (SellPortal p : plugin.getPortalManager().getPortals()) {
            if (top == p.getMainInventory())
                return new PortalContext(p, InventoryType.MAIN);
            if (top == p.getWhitelistInventory())
                return new PortalContext(p, InventoryType.WHITELIST);
            if (top == p.getBlacklistInventory())
                return new PortalContext(p, InventoryType.BLACKLIST);
        }

        if (view != null) {
            String title = view.getTitle();
            if (title == null) return null;

            String mainTitle = plugin.getConfigUtil().getGuiMainTitle();
            String whitelistTitle = plugin.getConfigUtil().getWhitelistTitle();
            String blacklistTitle = plugin.getConfigUtil().getBlacklistTitle();
            String boostersTitle = plugin.getConfigUtil().getBoostersGuiTitle();

            if (title.equals(whitelistTitle) || title.equals(blacklistTitle) || title.equals(mainTitle) || title.equals(boostersTitle)) {
                SellPortal p = findPortalByInventoryMatch(top);
                if (p != null) {
                    InventoryType t = title.equals(whitelistTitle) ? InventoryType.WHITELIST
                            : title.equals(blacklistTitle) ? InventoryType.BLACKLIST
                              : title.equals(boostersTitle) ? InventoryType.BOOSTER : InventoryType.MAIN;
                    return new PortalContext(p, t);
                }
            }
        }

        return null;
    }

    private void handleMainClick(Player player, SellPortal portal, int slot, boolean isTop, InventoryClickEvent event) {
        if (!isTop) {
            event.setCancelled(false);
            return;
        }

        if (portal.isBackButtonSlot(slot)) {
            player.closeInventory();
        } else if (portal.isWhitelistSlot(slot)) {
            player.openInventory(portal.buildWhitelistGUI());
        } else if (portal.isBlacklistSlot(slot)) {
            player.openInventory(portal.buildBlacklistGUI());
        } else if (portal.isPickupSlot(slot)) {
            player.closeInventory();
            plugin.getPortalManager().handlePickupPortal(player, portal);
        } else if (portal.isBoosterSlot(slot)) {
            player.openInventory(portal.buildBoostersGUI());
        }
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
                ItemStack moved = event.getCurrentItem();
                if (moved == null || moved.getType().isAir())
                    return;

                if (portal.isInOtherFilter(type, moved)) {
                    event.setCancelled(true);
                    return;
                }

                Inventory top = view.getTopInventory();
                int emptySlot = findEmptySlot(top, size - 9);
                if (emptySlot == -1)
                    return;

                ItemStack one = moved.clone();
                one.setAmount(1);
                top.setItem(emptySlot, one);
                playFilterSound((Player) event.getWhoClicked(), isWhitelist);
                return;
            }

            if (portal.isInOtherFilter(type, event.getCurrentItem())) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(false);
            return;
        }

        ItemStack cursor = event.getCursor();
        if (!cursor.getType().isAir()) {
            if (portal.isInOtherFilter(type, cursor)) {
                event.setCancelled(true);
                return;
            }

            ItemStack one = cursor.clone();
            one.setAmount(1);
            event.setCurrentItem(one);
            playFilterSound((Player) event.getWhoClicked(), isWhitelist);
            portal.markDirty();
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (current != null && !current.getType().isAir()) {
            event.setCurrentItem(null);
            playFilterSound((Player) event.getWhoClicked(), isWhitelist);
            portal.markDirty();
        }
    }

    private void handleBoosterClick(Player player, SellPortal portal, int slot) {
        if (plugin.getConfigUtil().getBackSlot() == slot) {
            portal.openMainMenu(player);
            return;
        }

        SellBooster booster = null;
        for (SellBooster b : plugin.getBoosterManager().getBoosterDefs()) {
            if (b.guiSlot() == slot) {
                booster = b;
                break;
            }
        }

        if (booster == null)
            return;

        String islandUUID = portal.getIslandUUID();
        if (islandUUID == null || islandUUID.isEmpty())
            return;

        plugin.getBoosterManager().activateBooster(player, islandUUID, booster);
        player.openInventory(portal.buildBoostersGUI());
    }

    private SellPortal findPortalByInventoryMatch(Inventory top) {
        for (SellPortal p : plugin.getPortalManager().getPortals()) {
            if (top == p.getMainInventory()
                    || top == p.getWhitelistInventory()
                    || top == p.getBlacklistInventory())
                return p;
        }

        return null;
    }

    private int findEmptySlot(Inventory inv, int limit) {
        for (int i = 0; i < limit; i++) {
            ItemStack it = inv.getItem(i);

            if (it == null || it.getType().isAir())
                return i;
        }

        return -1;
    }

    private void playFilterSound(Player player, boolean isWhitelist) {
        String soundName = isWhitelist
                ? plugin.getConfigUtil().getWhitelistSound()
                : plugin.getConfigUtil().getBlacklistSound();
        float volume = isWhitelist
                ? plugin.getConfigUtil().getWhitelistSoundVolume()
                : plugin.getConfigUtil().getBlacklistSoundVolume();
        float pitch = isWhitelist
                ? plugin.getConfigUtil().getWhitelistSoundPitch()
                : plugin.getConfigUtil().getBlacklistSoundPitch();

        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Inventory top = event.getView().getTopInventory();
        PortalContext ctx = detect(top);
        if (ctx == null)
            return;

        if (ctx.type() != InventoryType.MAIN) {
            int size = ctx.type() == InventoryType.WHITELIST
                    ? plugin.getConfigUtil().getWhitelistSize()
                    : plugin.getConfigUtil().getBlacklistSize();

            boolean touchesTop = event.getRawSlots().stream()
                    .anyMatch(slot -> slot >= 0 && slot < size);

            if (touchesTop)
                event.setCancelled(true);
        } else {
            event.setCancelled(true);
        }
    }

    private PortalContext detect(Inventory top) {
        return detect(top, null);
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
            case MAIN, BOOSTER -> {
            }
        }

        ctx.portal().save();
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private record PortalContext(SellPortal portal, InventoryType type) {
    }
}
