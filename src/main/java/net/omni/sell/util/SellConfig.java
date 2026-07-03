package net.omni.sell.util;

import net.omni.sell.OmniSell;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SellConfig {
    private final OmniSell plugin;
    private final File file;
    private final String configName;
    private FileConfiguration config;

    public SellConfig(OmniSell plugin, String fileName) {
        this.plugin = plugin;

        if (plugin.getDataFolder().mkdir())
            plugin.sendConsole("<green>Successfully created .../OmniSell</green>");

        this.configName = fileName;

        this.file = new File(plugin.getDataFolder(), fileName);

        reload();
    }

    public void reload() {
        if (!file.exists()) {
            try {
                plugin.saveResource(configName, false);
                plugin.sendConsole("<green>Successfully created " + configName + "</green>");
            } catch (Exception e) {
                plugin.getLogger().warning("Could not create " + configName);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void set(String path, Object object) {
        getConfig().set(path, object);
        save();
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.sendConsole("<red>Couldn't save " + configName + "</red>");
        }
    }

    public void setNoSave(String path, Object object) {
        getConfig().set(path, object);
    }

    public String getString(String path) {
        return getConfig().getString(path);
    }

    public List<String> getStringList(String path) {
        return getConfig().getStringList(path);
    }
}