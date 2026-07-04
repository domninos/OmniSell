package net.omni.sell.managers;

import net.kyori.adventure.text.Component;
import net.omni.sell.OmniSell;
import net.omni.sell.handlers.ActiveBooster;
import net.omni.sell.handlers.SellBooster;
import net.omni.sell.handlers.SellPortal;
import net.omni.sell.messages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BoosterManager {

    private final OmniSell plugin;
    private final List<SellBooster> boosterDefs = new ArrayList<>();
    private final Map<String, List<ActiveBooster>> activeBoosters = new ConcurrentHashMap<>();

    public BoosterManager(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadDefinitions();
        loadActiveFromDB();
        startCleanupTask();
    }

    private void loadDefinitions() {
        boosterDefs.clear();
        for (Map<String, Object> def : plugin.getConfigUtil().getSellBoosterDefs()) {
            String id = (String) def.get("id");
            if (id == null)
                continue;

            Material mat;
            try {
                mat = Material.valueOf(((String) def.get("material")).toUpperCase());
            } catch (Exception e) {
                mat = Material.GOLDEN_APPLE;
            }

            String rawDisplayName = (String) def.getOrDefault("display_name", "<gold>Booster</gold>");

            List<String> loreStr = (List<String>) def.getOrDefault("lore", new ArrayList<>());

            double multiplier = ((Number) def.getOrDefault("multiplier", 2.0)).doubleValue();
            long duration = ((Number) def.getOrDefault("duration", 1800)).longValue();
            long cooldown = ((Number) def.getOrDefault("cooldown", -1)).longValue();
            int slot = ((Number) def.getOrDefault("slot", 12)).intValue();

            boosterDefs.add(new SellBooster(id, mat, rawDisplayName, loreStr, multiplier, duration, cooldown, slot));
        }
        plugin.sendConsole("<green>Loaded " + boosterDefs.size() + " sell booster definitions.</green>");
    }

    private void loadActiveFromDB() {
        plugin.getDatabaseManager().loadActiveBoostersAsync().thenAccept(rows ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    int counter = 0;

                    for (Object[] row : rows) {
                        int dbId = (int) row[0];
                        String islandUUID = (String) row[1];
                        String boosterId = (String) row[2];
                        long expiryTime = (long) row[3];
                        long cooldownEnd = (long) row[4];

                        SellBooster def = getDefinition(boosterId);
                        if (def == null)
                            continue;

                        if (expiryTime != -1 && System.currentTimeMillis() >= expiryTime) {
                            plugin.getDatabaseManager().deleteActiveBooster(dbId);
                            continue;
                        }

                        ActiveBooster ab = new ActiveBooster(dbId, islandUUID, def, expiryTime, cooldownEnd);
                        activeBoosters.computeIfAbsent(islandUUID, k -> new ArrayList<>()).add(ab);

                        counter++;
                    }

                    plugin.sendConsole("<green>Loaded " + counter + " active boosters from database.</green>");
                }));
    }

    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<String, List<ActiveBooster>> entry : activeBoosters.entrySet()) {
                entry.getValue().removeIf(ab -> {
                    if (ab.isExpired()) {
                        plugin.getDatabaseManager().deleteActiveBooster(ab.dbId());
                        return true;
                    }

                    return false;
                });
            }
        }, 600L, 600L);
    }

    public SellBooster getDefinition(String id) {
        for (SellBooster b : boosterDefs) {
            if (b.id().equals(id))
                return b;
        }

        return null;
    }

    public List<SellBooster> getBoosterDefs() {
        return List.copyOf(boosterDefs);
    }

    public double getActiveMultiplier(String islandUUID) {
        if (islandUUID == null || islandUUID.isEmpty())
            return 1.0;

        List<ActiveBooster> boosters = activeBoosters.get(islandUUID);

        if (boosters == null || boosters.isEmpty())
            return 1.0;

        double total = 0.0;

        for (ActiveBooster ab : boosters) {
            if (!ab.isExpired())
                total += ab.definition().multiplier();
        }

        return Math.max(1.0, total);
    }

    public void activateBooster(Player player, String islandUUID, SellBooster booster) {
        if (isBoosterActive(islandUUID, booster.id())) {
            plugin.sendMessage(player, Messages.BOOSTER_ALREADY_ACTIVE.toString());
            return;
        }

        ActiveBooster existing = findActiveByDefinition(islandUUID, booster);
        if (existing != null && existing.isOnCooldown()) {
            plugin.sendMessage(player, Messages.BOOSTER_ON_COOLDOWN.toString()
                    .replace("%remaining%", booster.formatDuration(existing.getRemainingCooldown())));
            return;
        }

        long expiryTime = booster.durationSeconds() == -1 ? -1
                : System.currentTimeMillis() + (booster.durationSeconds() * 1000);
        long cooldownEnd = booster.cooldownSeconds() == -1 ? -1
                : System.currentTimeMillis() + (booster.cooldownSeconds() * 1000);

        plugin.getDatabaseManager().saveActiveBooster(islandUUID, booster.id(), expiryTime, cooldownEnd);

        ActiveBooster ab = new ActiveBooster(-1, islandUUID, booster, expiryTime, cooldownEnd);
        activeBoosters.computeIfAbsent(islandUUID, k -> new ArrayList<>()).add(ab);

        String durationStr = booster.durationSeconds() == -1
                ? "permanent"
                : booster.formatDuration(booster.durationSeconds() * 1000);

        plugin.sendMessage(player, Messages.BOOSTER_ACTIVATED.toString()
                .replace("%booster%", booster.id())
                .replace("%multiplier%", String.valueOf(booster.multiplier()))
                .replace("%duration%", durationStr));
    }

    public boolean isBoosterActive(String islandUUID, String boosterId) {
        List<ActiveBooster> boosters = activeBoosters.get(islandUUID);

        if (boosters == null)
            return false;

        return boosters.stream()
                .anyMatch(ab -> ab.definition().id().equals(boosterId) && !ab.isExpired());
    }

    private ActiveBooster findActiveByDefinition(String islandUUID, SellBooster booster) {
        List<ActiveBooster> boosters = activeBoosters.get(islandUUID);

        if (boosters == null)
            return null;

        for (ActiveBooster ab : boosters) {
            if (ab.definition().id().equals(booster.id()))
                return ab;
        }

        return null;
    }

    public void placeBoosterItems(SellPortal portal, Inventory gui) {
        String islandUUID = portal.getIslandUUID();

        for (SellBooster booster : boosterDefs) {
            ItemStack item = booster.createItem(plugin.getChatRenderer());
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                List<Component> lore = meta.lore();

                if (lore == null)
                    lore = new ArrayList<>();

                if (isBoosterActive(islandUUID, booster.id()))
                    lore.add(Component.text(plugin.getChatRenderer().parse("<green> ACTIVE</green>")));
                else
                    lore.add(Component.text(plugin.getChatRenderer().parse("<gray>Click to activate</gray>")));

                meta.lore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(booster.guiSlot(), item);
        }
    }

    public void shutdown() {
        activeBoosters.clear();
        boosterDefs.clear();
    }
}
