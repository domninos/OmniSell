package net.omni.sell.hooks;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.data.DatabaseBridge;
import com.bgsoftware.superiorskyblock.api.data.DatabaseBridgeMode;
import com.bgsoftware.superiorskyblock.api.data.DatabaseFilter;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.bank.IslandBank;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import net.omni.sell.OmniSell;
import net.omni.sell.handlers.SellPortal;
import net.omni.sell.messages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SuperiorSkyblock2Hook {

    private final OmniSell plugin;

    private boolean enabled = false;
    private final Set<UUID> dirtyIslands = ConcurrentHashMap.newKeySet();

    public SuperiorSkyblock2Hook(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void init() {
        this.enabled = true;

        startBatchProcessor();

        plugin.sendConsole("<green>Successfully hooked into SuperiorSkyblock2</green>");
    }

    public String checkIsland(Location anchor, Cancellable event, Player player) {
        if (!isEnabled())
            return "";

        Island island = SuperiorSkyblockAPI.getIslandAt(anchor);
        if (island == null) {
            event.setCancelled(true);
            plugin.sendMessage(player, "<red>You must place the portal on your island.</red>");
            return "";
        }

        String islandUUID = island.getUniqueId().toString();

        if (plugin.getPortalManager().hasPortalForIsland(islandUUID)) {
            event.setCancelled(true);
            plugin.sendMessage(player, Messages.PORTAL_ALREADY_EXISTS.toString());
            return "";
        }

        return islandUUID;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Player> getOnlineIslandMembers(String islandUUID) {
        try {
            Island island = SuperiorSkyblockAPI.getIslandByUUID(UUID.fromString(islandUUID));
            if (island == null) return List.of();

            List<Player> online = new ArrayList<>();
            for (SuperiorPlayer sp : island.getIslandMembers(true)) {
                if (sp.isOnline()) {
                    Player p = sp.asPlayer();
                    if (p != null) online.add(p);
                }
            }
            return online;
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    public boolean depositMoney(SellPortal portal, double price) {
        if (!isEnabled())
            return false;

        Island island = SuperiorSkyblockAPI.getIslandAt(portal.getLocation());

        if (island == null)
            return false;

        IslandBank islandBank = island.getIslandBank();
        BigDecimal bigDecimal = BigDecimal.valueOf(price);

        if (!islandBank.canDepositMoney(bigDecimal))
            return false;

        islandBank.setBalance(islandBank.getBalance().add(bigDecimal));
        dirtyIslands.add(island.getUniqueId());

        return true;
    }

    private void startBatchProcessor() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (dirtyIslands.isEmpty())
                return;

            Set<UUID> batch = new HashSet<>(dirtyIslands);
            dirtyIslands.clear();

            for (UUID islandUUID : batch) {
                Island island = SuperiorSkyblockAPI.getIslandByUUID(islandUUID);

                if (island == null)
                    continue;

                DatabaseBridge bridge = island.getDatabaseBridge();
                bridge.updateObject(
                        "islands_banks",
                        DatabaseFilter.fromFilter("island", island.getUniqueId().toString()),
                        new Pair<>("balance", island.getIslandBank().getBalance())
                );
            }
        }, 600L, 600L);
    }

    public void shutdown() {
        if (!dirtyIslands.isEmpty()) {
            for (UUID islandUUID : dirtyIslands) {
                Island island = SuperiorSkyblockAPI.getIslandByUUID(islandUUID);

                if (island == null)
                    continue;

                DatabaseBridge bridge = island.getDatabaseBridge();
                bridge.setDatabaseBridgeMode(DatabaseBridgeMode.SAVE_DATA);
                bridge.updateObject(
                        "islands_banks",
                        DatabaseFilter.fromFilter("island", island.getUniqueId().toString()),
                        new Pair<>("balance", island.getIslandBank().getBalance())
                );
            }
            dirtyIslands.clear();
        }
    }
}
