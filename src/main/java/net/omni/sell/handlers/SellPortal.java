package net.omni.sell.handlers;

import net.omni.sell.OmniSell;
import net.omni.sell.chat.ChatRenderer;
import net.omni.sell.util.InventoryType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SellPortal {

    private final OmniSell plugin;
    private final ChatRenderer renderer;
    private final Location location;
    private final UUID ownerUUID;
    private final int size;
    private final Inventory mainInventory;
    private final Inventory whitelistInventory;
    private final Inventory blacklistInventory;

    private List<String> frameKeys = List.of();
    private String islandUUID;
    private boolean dirty = false;

    public SellPortal(Location location, UUID ownerUUID, int size, String islandUUID, OmniSell plugin) {
        this(location, ownerUUID, size, plugin);
        this.islandUUID = islandUUID;
    }

    public SellPortal(Location location, UUID ownerUUID, int size, OmniSell plugin) {
        this.plugin = plugin;
        this.renderer = plugin.getChatRenderer();
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.size = size;

        this.mainInventory = renderer.createInventory(
                new SellPortalHolder(this, InventoryType.MAIN),
                plugin.getConfigUtil().getGuiMainSize(),
                plugin.getConfigUtil().getGuiMainTitle());

        this.whitelistInventory = renderer.createInventory(
                new SellPortalHolder(this, InventoryType.WHITELIST),
                plugin.getConfigUtil().getWhitelistSize(),
                plugin.getConfigUtil().getWhitelistTitle());

        this.blacklistInventory = renderer.createInventory(
                new SellPortalHolder(this, InventoryType.BLACKLIST),
                plugin.getConfigUtil().getBlacklistSize(),
                plugin.getConfigUtil().getBlacklistTitle());

        setupMainButtons();
    }

    private void setupMainButtons() {
        int size = mainInventory.getSize();
        ItemStack filler = createItem(
                plugin.getConfigUtil().getFillerMat(),
                plugin.getConfigUtil().getFillerDisplayName());

        for (int i = 0; i < size; i++)
            mainInventory.setItem(i, filler.clone());

        mainInventory.setItem(plugin.getConfigUtil().getWhitelistSlot(),
                createItem(plugin.getConfigUtil().getWhitelistMat(),
                        plugin.getConfigUtil().getWhitelistDisplayName(),
                        plugin.getConfigUtil().getWhitelistLore()));

        mainInventory.setItem(plugin.getConfigUtil().getBlacklistSlot(),
                createItem(plugin.getConfigUtil().getBlacklistMat(),
                        plugin.getConfigUtil().getBlacklistDisplayName(),
                        plugin.getConfigUtil().getBlacklistLore()));

        mainInventory.setItem(plugin.getConfigUtil().getPickupSlot(),
                createItem(plugin.getConfigUtil().getPickupMat(),
                        plugin.getConfigUtil().getPickupDisplayName(),
                        plugin.getConfigUtil().getPickupLore()));

        mainInventory.setItem(plugin.getConfigUtil().getBoosterSlot(),
                createItem(plugin.getConfigUtil().getBoosterMat(),
                        plugin.getConfigUtil().getBoosterDisplayName(),
                        plugin.getConfigUtil().getBoosterLore()));

        mainInventory.setItem(plugin.getConfigUtil().getBackSlot(),
                createItem(plugin.getConfigUtil().getBackButtonMat(),
                        plugin.getConfigUtil().getBackButtonDisplayName(),
                        plugin.getConfigUtil().getBackButtonLore()));
    }

    private ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (name != null && !name.isEmpty())
            renderer.setDisplayName(meta, name);

        if (lore != null && !lore.isEmpty())
            renderer.setLore(meta, lore);

        item.setItemMeta(meta);
        return item;
    }

    public void loadItems(List<ItemStack> whitelist, List<ItemStack> blacklist) {
        fillSlots(whitelistInventory, whitelist, whitelistInventory.getSize() - 9);
        fillSlots(blacklistInventory, blacklist, blacklistInventory.getSize() - 9);
    }

    private void fillSlots(Inventory inv, List<ItemStack> items, int limit) {
        for (int i = 0; i < Math.min(items.size(), limit); i++) {
            ItemStack it = items.get(i);

            if (it != null && it.getType() != Material.AIR)
                inv.setItem(i, it);
        }
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public int getSize() {
        return size;
    }

    public Inventory getMainInventory() {
        return mainInventory;
    }

    public Inventory getWhitelistInventory() {
        return whitelistInventory;
    }

    public Inventory getBlacklistInventory() {
        return blacklistInventory;
    }

    public String getIslandUUID() {
        return islandUUID;
    }

    public void setIslandUUID(String islandUUID) {
        this.islandUUID = islandUUID;
    }

    public List<String> getFrameKeys() {
        return frameKeys;
    }

    public void setFrameKeys(List<String> frameKeys) {
        this.frameKeys = frameKeys;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean shouldCollect(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

        boolean hasWhitelist = hasItems(whitelistInventory);
        boolean hasBlacklist = hasItems(blacklistInventory);

        if (hasWhitelist)
            return isInFilter(whitelistInventory, item);

        if (hasBlacklist)
            return !isInFilter(blacklistInventory, item);

        return true;
    }

    private boolean hasItems(Inventory inventory) {
        int itemSlots = inventory.getSize() - 9;

        for (int i = 0; i < itemSlots; i++) {
            ItemStack it = inventory.getItem(i);

            if (it != null && it.getType() != Material.AIR)
                return true;
        }

        return false;
    }

    private boolean isInFilter(Inventory filter, ItemStack item) {
        int itemSlots = filter.getSize() - 9;

        for (int i = 0; i < itemSlots; i++) {
            ItemStack filterItem = filter.getItem(i);

            if (filterItem != null && filterItem.isSimilar(item))
                return true;
        }

        return false;
    }

    public boolean isInOtherFilter(InventoryType type, ItemStack item) {
        Inventory other = type == InventoryType.WHITELIST ? blacklistInventory : whitelistInventory;
        return isInFilter(other, item);
    }

    public void openMainMenu(Player player) {
        player.closeInventory();
        setupMainButtons();
        player.openInventory(mainInventory);
    }

    public Inventory buildWhitelistGUI() {
        return buildFilterGUI(
                plugin.getConfigUtil().getWhitelistSize(),
                plugin.getConfigUtil().getWhitelistTitle(),
                whitelistInventory,
                InventoryType.WHITELIST);
    }

    private Inventory buildFilterGUI(int size, String title,
                                     Inventory filterSource, InventoryType type) {
        Inventory gui = renderer.createInventory(
                new SellPortalHolder(this, type), size, title);

        int itemSlots = size - 9;
        ItemStack filler = createItem(
                plugin.getConfigUtil().getFillerMat(),
                plugin.getConfigUtil().getFillerDisplayName());

        for (int i = itemSlots; i < size; i++)
            gui.setItem(i, filler.clone());

        if (plugin.getConfigUtil().getBackSlot() < size)
            gui.setItem(plugin.getConfigUtil().getBackSlot(),
                    createItem(plugin.getConfigUtil().getBackButtonMat(),
                            plugin.getConfigUtil().getBackButtonDisplayName(),
                            plugin.getConfigUtil().getBackButtonLore()));

        for (int i = 0; i < Math.min(itemSlots, filterSource.getSize() - 9); i++) {
            ItemStack it = filterSource.getItem(i);

            if (it != null && it.getType() != Material.AIR)
                gui.setItem(i, it.clone());
        }

        return gui;
    }

    public Inventory buildBlacklistGUI() {
        return buildFilterGUI(
                plugin.getConfigUtil().getBlacklistSize(),
                plugin.getConfigUtil().getBlacklistTitle(),
                blacklistInventory,
                InventoryType.BLACKLIST);
    }

    public void applyWhitelistChanges(Inventory view) {
        applyFilterChanges(view, whitelistInventory);
    }

    private void applyFilterChanges(Inventory view, Inventory target) {
        int limit = Math.min(view.getSize() - 9, target.getSize() - 9);
        Material fillerMat = plugin.getConfigUtil().getFillerMat();
        int backSlot = plugin.getConfigUtil().getBackSlot();
        for (int i = 0; i < limit; i++) {
            if (i == backSlot) continue;
            ItemStack item = view.getItem(i);
            if (item != null && item.getType() != Material.AIR && item.getType() != fillerMat)
                target.setItem(i, item);
            else
                target.setItem(i, null);
        }
        for (int i = limit; i < target.getSize() - 9; i++)
            target.setItem(i, null);
        this.dirty = true;
    }

    public void applyBlacklistChanges(Inventory view) {
        applyFilterChanges(view, blacklistInventory);
    }

    public boolean isWhitelistSlot(int slot) {
        return slot == plugin.getConfigUtil().getWhitelistSlot();
    }

    public boolean isBlacklistSlot(int slot) {
        return slot == plugin.getConfigUtil().getBlacklistSlot();
    }

    public boolean isBackButtonSlot(int slot) {
        return slot == plugin.getConfigUtil().getBackSlot();
    }

    public boolean isPickupSlot(int slot) {
        return slot == plugin.getConfigUtil().getPickupSlot();
    }

    public boolean isBoosterSlot(int slot) {
        return slot == plugin.getConfigUtil().getBoosterSlot();
    }

    // TODO cache
    public Inventory buildBoostersGUI() {
        int size = plugin.getConfigUtil().getBoostersGuiSize();

        Inventory gui = renderer.createInventory(
                new SellPortalHolder(this, InventoryType.BOOSTER),
                size,
                plugin.getConfigUtil().getBoostersGuiTitle());

        ItemStack filler = createItem(
                plugin.getConfigUtil().getFillerMat(),
                plugin.getConfigUtil().getFillerDisplayName());

        for (int i = 0; i < size; i++)
            gui.setItem(i, filler.clone());

        if (plugin.getConfigUtil().getBackSlot() < size)
            gui.setItem(plugin.getConfigUtil().getBackSlot(),
                    createItem(plugin.getConfigUtil().getBackButtonMat(),
                            plugin.getConfigUtil().getBackButtonDisplayName(),
                            plugin.getConfigUtil().getBackButtonLore()));

        plugin.getBoosterManager().placeBoosterItems(this, gui);

        return gui;
    }

    public void save() {
        if (!dirty) return;

        List<ItemStack> whitelistItems = extractItems(whitelistInventory, whitelistInventory.getSize() - 9);
        List<ItemStack> blacklistItems = extractItems(blacklistInventory, blacklistInventory.getSize() - 9);

        plugin.getDatabaseManager().saveFull(
                location, ownerUUID.toString(), size, frameKeys, islandUUID, whitelistItems, blacklistItems);
        dirty = false;

        whitelistItems.clear(); // garbage
        blacklistItems.clear(); // garbage
    }

    private List<ItemStack> extractItems(Inventory inv, int limit) {
        List<ItemStack> result = new ArrayList<>();

        for (int i = 0; i < Math.min(limit, inv.getSize()); i++) {
            ItemStack it = inv.getItem(i);

            if (it != null && it.getType() != Material.AIR)
                result.add(it);
        }

        return result;
    }

    public void flush() {
        mainInventory.clear();
        whitelistInventory.clear();
        blacklistInventory.clear();
        this.dirty = false;
    }
}
