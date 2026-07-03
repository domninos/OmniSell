package net.omni.sell.hooks;

import net.omni.sell.OmniSell;

public class SuperiorSkyblock2Hook {

    private final OmniSell plugin;

    private boolean enabled = false;

    public SuperiorSkyblock2Hook(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void init() {
        this.enabled = true;

        plugin.sendConsole("<green>Successfully hooked into SuperiorSkyblock2</green>");
    }

    public boolean isEnabled() {
        return enabled;
    }
}
