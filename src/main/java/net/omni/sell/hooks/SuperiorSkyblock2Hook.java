package net.omni.sell.hooks;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.data.DatabaseBridge;
import com.bgsoftware.superiorskyblock.api.data.DatabaseBridgeMode;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.bank.IslandBank;
import net.omni.sell.OmniSell;
import net.omni.sell.handlers.SellPortal;
import net.omni.sell.messages.Messages;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import java.math.BigDecimal;

public class SuperiorSkyblock2Hook {

    private final OmniSell plugin;

    private boolean enabled = false;

    public SuperiorSkyblock2Hook(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void init() {
        this.enabled = true;

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


//        islandBank.depositAdminMoney(Bukkit.getConsoleSender(), bigDecimal); // this makes a ton of transaction logs

        DatabaseBridge bridge = island.getDatabaseBridge();

        // doesn't work
        bridge.setDatabaseBridgeMode(DatabaseBridgeMode.SAVE_DATA);
        islandBank.setBalance(islandBank.getBalance().add(bigDecimal));

        // run this on async if using this, otherwise, get IslandsDatabaseBridge.saveBankBalance(island) somehow
//            bridge.updateObject(
//                    "islands_banks",
//                    DatabaseFilter.fromFilter("island", island.getUniqueId().toString()),
//                    new Pair<>("balance", islandBank.getBalance())
//            );
        return true;
    }
}
