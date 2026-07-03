package net.omni.sell.hooks;

import net.omni.sell.OmniSell;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;
import su.nightexpress.excellenteconomy.api.currency.operation.NotificationTarget;
import su.nightexpress.excellenteconomy.api.currency.operation.OperationContext;
import su.nightexpress.excellenteconomy.user.CoinsUser;

public class ExcellentEconomyHook {

    private final OmniSell plugin;

    private boolean enabled = false;
    private ExcellentEconomyAPI api;

    public ExcellentEconomyHook(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void init() {
        RegisteredServiceProvider<ExcellentEconomyAPI> provider = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);

        if (provider != null) {
            this.api = provider.getProvider();

            plugin.sendConsole("<green>Successfully hooked into ExcellentEconomyAPI");

            this.enabled = true;

            OperationContext.custom("OmniSell")
                    .silentFor(NotificationTarget.USER, NotificationTarget.EXECUTOR, NotificationTarget.CONSOLE_LOGGER);

//            myContext.
        }
    }

    // USE FOR UPGRADING
    public void removeMoney(Player player, ExcellentCurrency currency, int amount) {
        if (!canPerform()) {
            plugin.sendConsole("Could not remove money at this state.");
            return;
        }

        CoinsUser user = api.getCachedUserData(player);

        user.removeBalance(currency, amount);
    }

    public boolean canPerform() {
        return isEnabled() && api.canPerformOperations();
    }

    public boolean isEnabled() {
        return enabled && api != null;
    }
}
