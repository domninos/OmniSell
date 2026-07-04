package net.omni.sell.handlers;

import net.omni.sell.util.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public record SellPortalHolder(SellPortal portal, InventoryType type) implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
        return switch (type) {
            case MAIN, BOOSTER -> portal.getMainInventory();
            case WHITELIST -> portal.getWhitelistInventory();
            case BLACKLIST -> portal.getBlacklistInventory();
        };
    }
}
