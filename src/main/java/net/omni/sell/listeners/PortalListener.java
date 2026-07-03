package net.omni.sell.listeners;

import net.omni.sell.OmniSell;
import net.omni.sell.handlers.SellPortal;
import net.omni.sell.util.Messages;
import net.omni.sell.util.Prices;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.EndPortalFrame;
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
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

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

        // TODO check for per island instead of per player
//        int existing = plugin.getDatabaseManager().countPortalsSync(player.getUniqueId());
//        if (existing > 0) {
//            event.setCancelled(true);
//            plugin.sendMessage(player, "<red>You already have a portal.</red>");
//            return;
//        }

        if (!hasSpace(anchor, portalSize)) {
            event.setCancelled(true);
            plugin.sendMessage(player, Messages.PORTAL_NO_SPACE.toString());
            return;
        }

        if (!generatePortalStructure(anchor, portalSize, player.getUniqueId())) {
            event.setCancelled(true);
            plugin.sendMessage(player, "<red>Could not generate portal structure.</red>");
            return;
        }

        event.setCancelled(true);
        player.getInventory().getItemInMainHand().setAmount(
                player.getInventory().getItemInMainHand().getAmount() - 1);

        SellPortal portal = new SellPortal(anchor, player.getUniqueId(), portalSize, plugin);
        plugin.getPortalManager().registerPortal(anchor, portal);
        plugin.getDatabaseManager().saveFull(anchor, player.getUniqueId().toString(), portalSize, List.of(), List.of());

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

    private boolean generatePortalStructure(Location anchor, int portalSize, UUID ownerUUID) {
        World world = anchor.getWorld();
        if (world == null) return false;

        int bx = anchor.getBlockX();
        int by = anchor.getBlockY();
        int bz = anchor.getBlockZ();

        int frameSide = portalSize + 2;
        int nz = bz - frameSide + 1;

        // Place frame blocks on the border
        for (int i = 0; i < frameSide; i++) {
            for (int j = 0; j < frameSide; j++) {
                int x = bx + i;
                int z = bz - j;

                boolean onSouth = j == 0;
                boolean onNorth = j == frameSide - 1;
                boolean onWest = i == 0;
                boolean onEast = i == frameSide - 1;

                // Only place frame blocks on edges, not corners
                boolean isEdge = (onSouth || onNorth || onWest || onEast);
                boolean isCorner = (onSouth || onNorth) && (onWest || onEast);

                if (isEdge && !isCorner) {
                    Block frameBlock = world.getBlockAt(x, by, z);
                    frameBlock.setType(Material.END_PORTAL_FRAME, false);

                    BlockState state = frameBlock.getState();
                    if (state instanceof org.bukkit.block.TileState tile) {
                        PersistentDataContainer pdc = tile.getPersistentDataContainer();
                        pdc.set(plugin.getPortalManager().getPortalAnchorKey(),
                                PersistentDataType.STRING, locationKey(anchor));
                        tile.update(true, false);
                    }
                } else if (i > 0 && i < frameSide - 1 && j > 0 && j < frameSide - 1) {
                    // Interior: fill with END_PORTAL
                    Block portalBlock = world.getBlockAt(x, by, z);
                    portalBlock.setType(Material.END_PORTAL, false);
                }
            }
        }

        return true;
    }

    private String locationKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.END_PORTAL_FRAME) return;

        System.out.println("break");

        Location anchor = getAnchorFromBlock(block);
        if (anchor == null) return;

        System.out.println("anchor found");

        Player player = event.getPlayer();
        SellPortal portal = plugin.getPortalManager().getPortal(anchor);
        if (portal == null) {
            portal = plugin.getDatabaseManager().loadPortalSync(anchor);
            if (portal != null)
                plugin.getPortalManager().registerPortal(anchor, portal);
        }

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

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        for (BlockState state : chunk.getTileEntities()) {
            if (state.getType() != Material.END_PORTAL_FRAME) continue;
            if (!(state instanceof TileState tile)) continue;

            PersistentDataContainer pdc = tile.getPersistentDataContainer();
            String key = pdc.get(plugin.getPortalManager().getPortalAnchorKey(), PersistentDataType.STRING);
            if (key == null) continue;

            Location anchor = locationFromKey(key);
            if (anchor == null || plugin.getPortalManager().hasPortal(anchor)) continue;

            SellPortal portal = plugin.getDatabaseManager().loadPortalSync(anchor);
            if (portal != null)
                plugin.getPortalManager().registerPortal(anchor, portal);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        plugin.getPortalManager().saveAndUnload(event.getChunk());
    }

    private Location getAnchorFromBlock(Block block) {
        if (block.getType() != Material.END_PORTAL_FRAME)
            return null;

        System.out.println("passed 1");

        if (!(block.getState() instanceof TileState tile))
            return null;

        System.out.println("getting anchor");

        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        String key = pdc.get(plugin.getPortalManager().getPortalAnchorKey(), PersistentDataType.STRING);
        if (key == null || key.isEmpty()) return null;

        return locationFromKey(key);
    }

    private void removePortalStructure(Location anchor, int portalSize) {
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

    private Location locationFromKey(String key) {
        String[] parts = key.split(":");
        if (parts.length < 4) return null;
        return new Location(
                plugin.getServer().getWorld(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.END_PORTAL_FRAME) return;

        System.out.println("interact");

        Location anchor = getAnchorFromBlock(event.getClickedBlock());
        if (anchor == null) return;

        System.out.println("no anchor");

        event.setCancelled(true);

        SellPortal portal = plugin.getPortalManager().getPortal(anchor);
        if (portal == null) {
            portal = plugin.getDatabaseManager().loadPortalSync(anchor);
            if (portal != null)
                plugin.getPortalManager().registerPortal(anchor, portal);
        }

        System.out.println(portal);
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
        if (!(entity instanceof Item item)) return;

        Location to = event.getTo();
        if (to == null) return;

        if (!plugin.getPortalManager().isGloballyEnabled()) return;

        // Find which portal this is
        SellPortal portal = findPortalNear(to);
        if (portal == null) return;

        ItemStack itemStack = item.getItemStack();
        if (itemStack.getType().isAir()) return;

        if (!portal.shouldCollect(itemStack)) return;

        String materialName = itemStack.getType().name();
        double price = 0;

        try {
            Prices prices = Prices.valueOf(materialName);
            price = prices.getPrice() * itemStack.getAmount();
        } catch (IllegalArgumentException e) {
            return;
        }

        if (price <= 0) return;

        if (plugin.getExcellentEconomyHook() != null && plugin.getExcellentEconomyHook().isEnabled()) {
            Player owner = plugin.getServer().getPlayer(portal.getOwnerUUID());
            if (owner != null && owner.isOnline())
                plugin.getExcellentEconomyHook().addMoney(owner, price);
        }

        // TODO use superiorskyblock island balance

        item.remove();
        event.setCancelled(true);
    }

    private SellPortal findPortalNear(Location location) {
        for (SellPortal portal : plugin.getPortalManager().getPortals()) {
            Location pl = portal.getLocation();
            if (pl.getWorld().equals(location.getWorld())) {
                int dx = Math.abs(pl.getBlockX() - location.getBlockX());
                int dz = Math.abs(pl.getBlockZ() - location.getBlockZ());
                int frameSide = portal.getSize() + 2;
                if (dx <= frameSide && dz <= frameSide)
                    return portal;
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
