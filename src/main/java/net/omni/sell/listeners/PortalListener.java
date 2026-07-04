package net.omni.sell.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.bank.IslandBank;
import net.omni.sell.OmniSell;
import net.omni.sell.handlers.SellPortal;
import net.omni.sell.messages.Messages;
import net.omni.sell.util.Prices;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PortalListener implements Listener {

    private final OmniSell plugin;

    public PortalListener(OmniSell plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getSellItemHandler().isSellPortal(event.getItemInHand()))
            return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Location anchor = block.getLocation();

        int portalSize = plugin.getConfigUtil().getPortalSize();

        if (portalSize < 1) {
            event.setCancelled(true);
            plugin.sendMessage(player, "<red>Invalid portal size configured.</red>");
            return;
        }

        if (plugin.getPortalManager().hasPortal(anchor)) {
            event.setCancelled(true);
            plugin.sendMessage(player, Messages.PORTAL_DISABLED.toString());
            return;
        }


        String islandUUID = null;
        if (plugin.getSuperiorSkyblock2Hook().isEnabled()) {
            Island island = SuperiorSkyblockAPI.getIslandAt(anchor);
            if (island == null) {
                event.setCancelled(true);
                plugin.sendMessage(player, "<red>You must place the portal on your island.</red>");
                return;
            }
            islandUUID = island.getUniqueId().toString();
            if (plugin.getPortalManager().hasPortalForIsland(islandUUID)) {
                event.setCancelled(true);
                plugin.sendMessage(player, Messages.PORTAL_ALREADY_EXISTS.toString());
                return;
            }
        }

        if (!hasSpace(anchor, portalSize)) {
            event.setCancelled(true);
            plugin.sendMessage(player, Messages.PORTAL_NO_SPACE.toString());
            return;
        }

        List<String> frameKeys = generatePortalStructure(anchor, portalSize);
        if (frameKeys == null) {
            event.setCancelled(true);
            plugin.sendMessage(player, "<red>Could not generate portal structure.</red>");
            return;
        }

        event.setCancelled(true);
        player.getInventory().getItemInMainHand().setAmount(
                player.getInventory().getItemInMainHand().getAmount() - 1);

        SellPortal portal = new SellPortal(anchor, player.getUniqueId(), portalSize, islandUUID, plugin);
        portal.setFrameKeys(frameKeys);
        plugin.getPortalManager().registerPortal(anchor, portal);
        plugin.getDatabaseManager().saveFull(anchor, player.getUniqueId().toString(), portalSize, frameKeys, islandUUID, List.of(), List.of());

        plugin.sendMessage(player, Messages.PORTAL_CREATED.toString());
    }

    private boolean hasSpace(Location anchor, int portalSize) {
        World world = anchor.getWorld();
        if (world == null) return false;

        int bx = anchor.getBlockX();
        int by = anchor.getBlockY();
        int bz = anchor.getBlockZ();
        int frameSide = portalSize + 2;

        for (int i = 0; i < frameSide; i++) {
            for (int j = 0; j < frameSide; j++) {
                boolean isCorner = (j == 0 || j == frameSide - 1) && (i == 0 || i == frameSide - 1);
                if (isCorner) continue;

                if (!world.getBlockAt(bx + i, by, bz - j).getType().isAir())
                    return false;
            }
        }
        return true;
    }

    private List<String> generatePortalStructure(Location anchor, int portalSize) {
        World world = anchor.getWorld();
        if (world == null) return null;

        int bx = anchor.getBlockX();
        int by = anchor.getBlockY();
        int bz = anchor.getBlockZ();

        int frameSide = portalSize + 2;
        List<String> frameKeys = new ArrayList<>();

        for (int i = 0; i < frameSide; i++) {
            for (int j = 0; j < frameSide; j++) {
                int x = bx + i;
                int z = bz - j;

                boolean onSouth = j == 0;
                boolean onNorth = j == frameSide - 1;
                boolean onWest = i == 0;
                boolean onEast = i == frameSide - 1;

                boolean isEdge = (onSouth || onNorth || onWest || onEast);
                boolean isCorner = (onSouth || onNorth) && (onWest || onEast);

                if (isEdge && !isCorner) {
                    Block frameBlock = world.getBlockAt(x, by, z);
                    frameBlock.setType(Material.END_PORTAL_FRAME, false);
                    frameKeys.add(locationKey(frameBlock.getLocation()));
                } else if (i > 0 && i < frameSide - 1 && j > 0 && j < frameSide - 1) {
                    Block portalBlock = world.getBlockAt(x, by, z);
                    portalBlock.setType(Material.END_PORTAL, false);
                }
            }
        }

        return frameKeys;
    }

    private String locationKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.END_PORTAL_FRAME) return;

        Location anchor = getAnchorFromBlock(block);
        if (anchor == null) return;

        Player player = event.getPlayer();
        SellPortal portal = plugin.getPortalManager().getPortal(anchor);
        if (portal == null) {
            event.setCancelled(true);
            return;
        }

        if (!portal.getOwnerUUID().equals(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.sendMessage(player, Messages.NO_PERMS.toString());
            return;
        }

        removePortalStructure(anchor, portal.getSize());
        plugin.getPortalManager().unregisterPortal(anchor);
        plugin.getDatabaseManager().deleteLocationSync(anchor);

        ItemStack portalItem = plugin.getSellItemHandler().getItemStack(1);
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), portalItem);

        event.setCancelled(true);
        block.setType(Material.AIR, false);

        plugin.sendMessage(player, Messages.PORTAL_REMOVED.toString());
    }

    private Location getAnchorFromBlock(Block block) {
        if (block.getType() != Material.END_PORTAL_FRAME)
            return null;
        return plugin.getPortalManager().getAnchorFromFrame(locationKey(block.getLocation()));
    }

    public static void removePortalStructure(Location anchor, int portalSize) {
        World world = anchor.getWorld();
        if (world == null) return;

        int bx = anchor.getBlockX();
        int by = anchor.getBlockY();
        int bz = anchor.getBlockZ();
        int frameSide = portalSize + 2;

        for (int i = 0; i < frameSide; i++) {
            for (int j = 0; j < frameSide; j++) {
                Block b = world.getBlockAt(bx + i, by, bz - j);
                if (b.getType() == Material.END_PORTAL_FRAME || b.getType() == Material.END_PORTAL)
                    b.setType(Material.AIR, false);
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        plugin.getPortalManager().saveAndUnload(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.END_PORTAL_FRAME) return;

        Location anchor = getAnchorFromBlock(event.getClickedBlock());
        if (anchor == null) return;

        event.setCancelled(true);

        SellPortal portal = plugin.getPortalManager().getPortal(anchor);
        if (portal == null) return;

        if (!plugin.getPortalManager().isGloballyEnabled()) {
            plugin.sendMessage(event.getPlayer(), Messages.PORTAL_DISABLED_GLOBAL.toString());
            return;
        }

        Player player = event.getPlayer();
        if (!portal.getOwnerUUID().equals(player.getUniqueId())) {
            plugin.sendMessage(player, Messages.NO_PERMS.toString());
            return;
        }

        portal.openMainMenu(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPortalEnter(EntityPortalEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Item item))
            return;


        if (!plugin.getPortalManager().isGloballyEnabled())
            return;

        Location loc = event.getFrom();
        SellPortal portal = findPortalNear(loc);
        if (portal == null)
            return;

        ItemStack itemStack = item.getItemStack();
        if (itemStack.getType().isAir())
            return;

        if (!portal.shouldCollect(itemStack))
            return;

        String materialName = itemStack.getType().name();
        double price;

        try {
            Prices prices = Prices.valueOf(materialName);
            price = prices.getPrice() * itemStack.getAmount();
        } catch (IllegalArgumentException e) {
            return;
        }

        if (price <= 0)
            return;

        if (plugin.getSuperiorSkyblock2Hook().isEnabled()) {
            Island island = SuperiorSkyblockAPI.getIslandAt(portal.getLocation());

            if (island == null)
                return;

            IslandBank islandBank = island.getIslandBank();
            BigDecimal bigDecimal = BigDecimal.valueOf(price);

            if (!islandBank.canDepositMoney(bigDecimal))
                return;

            islandBank.depositAdminMoney(Bukkit.getConsoleSender(), bigDecimal);
        }

        item.remove();
        event.setCancelled(true);
    }

    private SellPortal findPortalNear(Location location) {
        World world = location.getWorld();
        if (world == null) return null;

        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();
        int radius = plugin.getConfigUtil().getPortalSize() + 2;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block b = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (b.getType() != Material.END_PORTAL_FRAME) continue;

                    Location anchor = plugin.getPortalManager()
                            .getAnchorFromFrame(locationKey(b.getLocation()));
                    if (anchor == null) continue;

                    SellPortal portal = plugin.getPortalManager().getPortal(anchor);
                    if (portal != null) return portal;
                }
            }
        }

        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerPortalEvent.TeleportCause.END_PORTAL) return;

        SellPortal portal = findPortalNear(event.getFrom());
        if (portal != null)
            event.setCancelled(true);
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
