package net.omni.sell;

import net.omni.sell.hooks.ExcellentEconomyHook;
import net.omni.sell.managers.DatabaseManager;
import net.omni.sell.managers.MessagesManager;
import net.omni.sell.util.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class OmniSell extends JavaPlugin {

    /*
        We make an end portal and any item that drops through the end portal automatically gets sold to the Skyblock Island Team that owns the portal

        Required hooks for this one would be
        ShopGUI+
        Skyblock Core
        CoinsEngine (Economy)

        max island size will be 250x250

        TODO
            1 portal per island it should be given as an item configurable
            .
            When the item is placed it place down 3x3 end portal with frames
             if the OWNER right clicks any of the frames it will give them the option to pick it.
            .
            Only 1 portal should be allowed to be placed per island
            .
            The portal upgrades:
             base: 3x3 and but have it configurable so if we wanna make it bigger we can
             sell boosters
     */

    private ChatRenderer chatRenderer;

    private DatabaseManager databaseManager;

    private ExcellentEconomyHook excellentEconomyHook;

    private MessagesManager messagesManager;
    private SellConfig messagesConfig;

    private ConfigUtil configUtil;

    @Override
    public void onDisable() {

        configUtil.flush();
        messagesManager.flush();

        databaseManager.closePool();


        sendConsole("<red>Successfully disabled.</red>");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initChatRenderer();

        this.messagesConfig = new SellConfig(this, "messages.yml");
        this.messagesManager = new MessagesManager(this);
        messagesManager.loadMessages();

        this.configUtil = new ConfigUtil(this);
        configUtil.load();

        registerHooks();
        registerCommands();

        registerListeners();

        sendConsole("<green>Successfully started " + getDescription().getName() + "-v" + getDescription().getVersion() + " </green>");
    }

    private void initChatRenderer() {
        try {
            Class.forName("net.kyori.adventure.text.Component");
            this.chatRenderer = new PaperChatRenderer();
            sendConsole("<green>PaperMC detected. Using PaperChatRenderer.</green>");
        } catch (ClassNotFoundException e) {
            this.chatRenderer = new SpigotChatRenderer();
            sendConsole("<gray>Spigot detected. Using SpigotChatRenderer.</gray>");
        }

        MessageUtil.init(chatRenderer);
    }

    private void registerHooks() {
        if (Bukkit.getPluginManager().isPluginEnabled("ExcellentEconomy")) {
            this.excellentEconomyHook = new ExcellentEconomyHook(this);

            excellentEconomyHook.init();
        }

        // TODO
    }

    private void registerCommands() {
    }

    private void registerListeners() {
    }

    public void sendConsole(String message) {
        chatRenderer.sendMessage(Bukkit.getConsoleSender(), chatRenderer.color(message));
    }

    public void sendMessage(CommandSender sender, String message) {
        chatRenderer.sendMessage(sender, chatRenderer.color(message));
    }

    public ExcellentEconomyHook getExcellentEconomyHook() {
        return excellentEconomyHook;
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public SellConfig getMessagesConfig() {
        return messagesConfig;
    }
}
