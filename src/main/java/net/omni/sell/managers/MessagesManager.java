package net.omni.sell.managers;
import net.omni.sell.OmniSell;
import net.omni.sell.messages.Messages;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class MessagesManager {
    private final OmniSell plugin;

    public MessagesManager(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        plugin.getMessagesConfig().reload();

        FileConfiguration config = plugin.getMessagesConfig().getConfig();

        int savedDefaults = 0;

        for (Messages message : Messages.values()) {
            if (message.getDefaultVal() instanceof List<?>) {
                if (!config.contains(message.getPath())) {
                    config.set(message.getPath(), message.getDefaultVal());
                    savedDefaults++;
                }

                message.setCachedVal(plugin.getConfig().getStringList(message.getPath()));
            } else {
                if (!config.contains(message.getPath())) {
                    config.set(message.getPath(), message.getDefaultVal());
                    savedDefaults++;
                }

                message.setCachedVal(config.getString(message.getPath()));
            }
        }

        if (savedDefaults > 0) {
            plugin.getMessagesConfig().save();

            plugin.sendConsole("<green>Successfully loaded " + savedDefaults + " default message(s).</green>");
        }
    }

    public void flush() {
        for (Messages message : Messages.values())
            message.flush();
    }
}