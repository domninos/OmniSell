package net.omni.sell.managers;

import net.omni.sell.OmniSell;
import net.omni.sell.util.Prices;
import org.bukkit.configuration.file.FileConfiguration;

public class PricesManager {
    private final OmniSell plugin;

    public PricesManager(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void loadPrices() {
        plugin.getPricesConfig().reload();

        FileConfiguration config = plugin.getPricesConfig().getConfig();

        int savedDefaults = 0;

        for (Prices price : Prices.values()) {
            if (!config.contains(price.getPath())) {
                config.set(price.getPath(), price.getDefaultPrice());
                savedDefaults++;
            }

            price.setCachedPrice(config.getDouble(price.getPath()));
        }

        if (savedDefaults > 0) {
            plugin.getPricesConfig().save();
            plugin.sendConsole("<green>Successfully loaded " + savedDefaults + " default price(s).</green>");
        }
    }

    public void flush() {
        for (Prices price : Prices.values())
            price.flush();
    }
}
