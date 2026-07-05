package net.omni.sell.messages;

import java.util.ArrayList;
import java.util.List;

public enum Messages {
    NO_PERMS("no_perms", "<red>You do not have permission to use this command.</red>"),
    ONLY_PLAYERS("only_players", "<red>Only players can use this command.</red>"),

    PLAYER_NOT_FOUND("player_not_found", "<red>Player %player% not found.</red>"),
    USAGE("usage", "<red>Invalid arguments. Usage: %usage%</red>"),
    UNKNOWN_COMMAND("unknown_cmd", "<red>Unknown command.</red>"),

    GIVE_SUCCESS("give_success", "<green>Successfully gave %player% a sell portal."),
    GIVE_ERROR("give.error", "<red>Could not give %player% a sell portal. Please check logs.</red>"),

    RELOADED("reloaded", "<green>config.yml, messages.yml, and prices.yml have been reloaded.</green>"),

    PORTAL_LIST_HEADER("portal_list_header", "\n<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>\n  <gradient:#00AAFF:#55FFFF>Sell Portals</gradient>\n"),
    PORTAL_LIST_ENTRY("portal_list_entry", "  <#00AAFF>%index%.</#00AAFF> <gray>%world%</gray> <dark_gray>at</dark_gray> <white>%x%,%y%,%z%</white> <dark_gray>-</dark_gray> %owner% %status%"),
    PORTAL_LIST_FOOTER("portal_list_footer", "<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>"),
    PORTAL_LIST_EMPTY("portal_list_empty", "<gray>No portals found.</gray>"),

    PORTAL_NOT_FOUND("portal_not_found", "<red>Portal not found at index %index%.</red>"),
    PORTAL_TP_SUCCESS("portal_tp_success", "<green>Teleported to portal #%index%.</green>"),

    PORTAL_DISABLED_GLOBAL("portal_disabled_global", "<red>All portals are currently disabled.</red>"),
    PORTAL_DISABLED("portal_disabled", "<red>That portal is disabled.</red>"),

    PORTAL_TOGGLE_GLOBAL("portal_toggle_global", "<green>Portals are now %state%.</green>"),
    PORTAL_TOGGLE("portal_toggle", "<green>Portal #%index% is now %state%.</green>"),

    PORTAL_ENABLED("portal_enabled", "<green>enabled</green>"),
    PORTAL_DISABLED_STATUS("portal_disabled_status", "<red>disabled</red>"),

    PORTAL_NO_SPACE("portal_no_space", "<red>Not enough space to place the portal.</red>"),
    PORTAL_CREATED("portal_created", "<green>Portal created!</green>"),
    PORTAL_REMOVED("portal_removed", "<green>Portal removed.</green>"),

    PORTAL_NO_EMPTY_SPACE("portal_no_empty_space", "<red>You need an empty inventory slot to pick up the portal.</red>"),
    PORTAL_PICKED_UP("portal_picked_up", "<green>Portal picked up!</green>"),
    PORTAL_ALREADY_EXISTS("portal_already_exists", "<red>This island already has a sell portal.</red>"),

    BOOSTER_ACTIVATED("booster_activated", "<green>Booster</green> <gold>%booster%</gold> <green>activated!</green> <gray>(%multiplier%x for %duration%)</gray>"),
    BOOSTER_ON_COOLDOWN("booster_on_cooldown", "<red>That booster is on cooldown.</red> <gray>%remaining% remaining.</gray>"),
    BOOSTER_ALREADY_ACTIVE("booster_already_active", "<red>That booster is already active on your island.</red>"),

    BOOSTER_NO_MONEY("booster_no_money", "<red>You need %cost% to activate this booster.</red>"),
    BOOSTER_ONLY_ONE("booster_only_one", "<red>Only one booster can be active at a time on your island.</red>");

    private final String path;
    private final Object defaultVal;
    private Object cachedVal;

    Messages(String path, Object defaultVal) {
        this.path = path;
        this.defaultVal = defaultVal;
    }

    public String getPath() {
        return path;
    }

    public Object getDefaultVal() {
        return defaultVal;
    }

    public void setCachedVal(Object val) {
        this.cachedVal = val;
    }

    public String replace(String... pairs) {
        String result = this.toString();

        return replace(result, pairs);
    }

    @Override
    public String toString() {
        if (cachedVal instanceof List<?>)
            return "";

        return cachedVal instanceof String ? (String) cachedVal : (String) defaultVal;
    }

    private String replace(String result, String... pairs) {
        if (result.isEmpty())
            return "";

        for (int i = 0; i < pairs.length - 1; i += 2) {
            String key = pairs[i];
            String val = pairs[i + 1];

            if (key != null && val != null) {
                result = result.replace("%" + key + "%", val);
            }
        }

        return result;
    }

    public String replaceList(String... pairs) {
        List<String> originalList = this.asList();

        if (originalList.isEmpty())
            return "";

        List<String> modifiedList = new ArrayList<>();

        for (String line : originalList) {
            if (line != null)
                modifiedList.add(replace(line, pairs));
        }

        return String.join("\n", modifiedList);
    }

    @SuppressWarnings("unchecked")
    public List<String> asList() {
        return cachedVal instanceof List<?> ? (List<String>) cachedVal : (List<String>) defaultVal;
    }

    public void flush() {
        if (cachedVal instanceof List<?> cachedList)
            cachedList.clear();

        if (defaultVal instanceof List<?> defaultList)
            defaultList.clear();

        this.cachedVal = null;
    }
}
