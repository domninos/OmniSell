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
    private String currencyId;

    public ExcellentEconomyHook(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void init() {
        RegisteredServiceProvider<ExcellentEconomyAPI> provider = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);

        if (provider != null) {
            this.api = provider.getProvider();
            this.context = OperationContext.custom("OmniSell")
                    .silentFor(NotificationTarget.USER, NotificationTarget.EXECUTOR, NotificationTarget.CONSOLE_LOGGER);
            this.currencyId = plugin.getConfig().getString("economy.currency", "coins");
            this.enabled = true;

            plugin.sendConsole("<green>Successfully hooked into ExcellentEconomy</green>");
        }
    }

    public void addMoney(Player player, double amount) {
        if (!canPerform()) return;
        api.deposit(player, currencyId, amount, context);
    }

    public Optional<ExcellentEconomyAPI> getApi() {
        return Optional.ofNullable(api);
    }

    public boolean canPerform() {
        return enabled && api.canPerformOperations();
    }

    public boolean isEnabled() {
        return enabled && api != null;
    }
}
