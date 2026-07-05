package net.omni.sell.hooks;

import net.omni.sell.OmniSell;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.operation.NotificationTarget;
import su.nightexpress.excellenteconomy.api.currency.operation.OperationContext;

import java.util.Optional;

public class ExcellentEconomyHook {

    private final OmniSell plugin;

    private boolean enabled = false;
    private ExcellentEconomyAPI api;
    private OperationContext context;

    public ExcellentEconomyHook(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void init() {
        RegisteredServiceProvider<ExcellentEconomyAPI> provider = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);

        if (provider != null) {
            this.api = provider.getProvider();
            this.context = OperationContext.custom("OmniSell")
                    .silentFor(NotificationTarget.USER, NotificationTarget.EXECUTOR, NotificationTarget.CONSOLE_LOGGER);
            this.enabled = true;

            plugin.sendConsole("<green>Successfully hooked into ExcellentEconomy.</green>");
        }
    }

    public boolean canPerform() {
        return isEnabled() && api.canPerformOperations();
    }

    public void removeMoney(Player player, String currencyId, double amount) {
        if (canPerform())
            api.withdraw(player, currencyId, amount, context);
    }

    public double getBalance(Player player, String currencyId) {
        return isEnabled() ? api.getBalance(player, currencyId) : -1;
    }

    public Optional<ExcellentEconomyAPI> getApi() {
        return Optional.ofNullable(api);
    }

    public boolean isEnabled() {
        return enabled && api != null;
    }
}
