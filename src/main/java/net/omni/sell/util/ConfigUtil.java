package net.omni.sell.util;

import net.omni.sell.OmniSell;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

public class ConfigUtil {
    private final OmniSell plugin;

    public ConfigUtil(OmniSell plugin) {
        this.plugin = plugin;
    }

    public boolean reloadConfig() {
        flush();

        plugin.reloadConfig();
        load();

        // TODO
        return true;
    }

    public void flush() {
    }

    public void load() {
        AtomicInteger savedDefaults = new AtomicInteger();

        if (savedDefaults.get() > 0) {
            plugin.saveConfig();

            plugin.sendConsole("<green>Successfully loaded " + savedDefaults.get() + " default configuration(s)</green>");
        }

        plugin.sendConsole("<green>Successfully loaded config.yml</green>");
    }

    private int getAndDefaultSlot(String path, int defaultVal, IntConsumer consumer) {
        int temp = plugin.getConfig().getInt(path);

        if (!plugin.getConfig().contains(path) || temp == 0) {
            plugin.getConfig().set(path, defaultVal);
            consumer.accept(1);
            return defaultVal;
        }

        return temp;
    }

    private String getAndDefaultString(String path, String defaultVal, IntConsumer consumer) {
        String temp = plugin.getConfig().getString(path);

        if (temp == null) {
            plugin.getConfig().set(path, defaultVal);
            consumer.accept(1);
            return defaultVal;
        }

        return temp;
    }
}
