package net.omni.sell.listeners;

import net.omni.sell.OmniSell;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;

public class PortalListener implements Listener {

    private final OmniSell plugin;

    public PortalListener(OmniSell plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityPortalEnter(EntityPortalEvent event) {
        //
//        event.getEntity().
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
}
