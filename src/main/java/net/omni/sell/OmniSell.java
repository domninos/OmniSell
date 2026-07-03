package net.omni.sell;

import net.omni.sell.chat.ChatRenderer;
import net.omni.sell.chat.PaperChatRenderer;
import net.omni.sell.chat.SpigotChatRenderer;
import net.omni.sell.commands.SellCommand;
import net.omni.sell.config.ConfigUtil;
import net.omni.sell.config.SellConfig;
import net.omni.sell.handlers.SellItemHandler;
import net.omni.sell.handlers.SellPortal;
import net.omni.sell.hooks.ExcellentEconomyHook;
import net.omni.sell.hooks.SuperiorSkyblock2Hook;
import net.omni.sell.listeners.GUIListener;
import net.omni.sell.listeners.PortalListener;
import net.omni.sell.managers.DatabaseManager;
import net.omni.sell.managers.MessagesManager;
import net.omni.sell.managers.PortalManager;
import net.omni.sell.managers.PricesManager;
import net.omni.sell.messages.MessageUtil;
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
            .
            softdepend SuperiorSkyblock2
                if hooked, deposit to island balance
                else, deposit to owner balance
            .
                make /sp list only show portal owned
     */

    private ChatRenderer chatRenderer;

    private DatabaseManager databaseManager;

    private ExcellentEconomyHook excellentEconomyHook;
    private SuperiorSkyblock2Hook superiorSkyblock2Hook;

    private MessagesManager messagesManager;
    private SellConfig messagesConfig;

    private PricesManager pricesManager;
    private SellConfig pricesConfig;

    private PortalManager portalManager;

    private SellItemHandler sellItemHandler;

    private ConfigUtil configUtil;

    @Override
    public void onDisable() {
        portalManager.saveDirty();

        configUtil.flush();
        messagesManager.flush();
        pricesManager.flush();
        portalManager.flush();
        sellItemHandler.flush();

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

        this.pricesConfig = new SellConfig(this, "prices.yml");
        this.pricesManager = new PricesManager(this);
        pricesManager.loadPrices();

        this.databaseManager = new DatabaseManager(this);
        databaseManager.initDatabase();

        this.portalManager = new PortalManager(this);

        this.configUtil = new ConfigUtil(this);
        configUtil.load();

        this.sellItemHandler = new SellItemHandler(this);
        sellItemHandler.load();

        registerHooks();
        registerCommands();

        sendConsole("<yellow>Registering portals..</yellow>");
        databaseManager.loadAllPortalsAsync().thenAccept(portals ->
                Bukkit.getScheduler().runTask(this, () -> {
                    for (SellPortal portal : portals)
                        portalManager.registerPortal(portal.getLocation(), portal);

                    registerListeners();
                    sendConsole("<green>Successfully registered portals.</green>");
                }));

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

        if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            this.superiorSkyblock2Hook = new SuperiorSkyblock2Hook(this);

            superiorSkyblock2Hook.init();
        }

        // TODO
    }

    private void registerCommands() {
        new SellCommand(this).register();
    }

    private void registerListeners() {
        new PortalListener(this).register();
        new GUIListener(this).register();
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

    public PricesManager getPricesManager() {
        return pricesManager;
    }

    public SellConfig getPricesConfig() {
        return pricesConfig;
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }

    public SuperiorSkyblock2Hook getSuperiorSkyblock2Hook() {
        return superiorSkyblock2Hook;
    }

    public ChatRenderer getChatRenderer() {
        return chatRenderer;
    }

    public SellItemHandler getSellItemHandler() {
        return sellItemHandler;
    }
}
