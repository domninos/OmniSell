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
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BoosterManager {

    private final OmniSell plugin;
    private final List<SellBooster> boosterDefs = new ArrayList<>();
    private final Map<String, List<ActiveBooster>> activeBoosters = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, ItemStack>> cachedBoosterItems = new ConcurrentHashMap<>();

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

            @SuppressWarnings("unchecked")
            List<String> loreStr = (List<String>) def.getOrDefault("lore", List.of());

            double multiplier = ((Number) def.getOrDefault("multiplier", 2.0)).doubleValue();
            long duration = ((Number) def.getOrDefault("duration", 1800)).longValue();
            long cooldown = ((Number) def.getOrDefault("cooldown", -1)).longValue();
            int slot = ((Number) def.getOrDefault("slot", 12)).intValue();

            @SuppressWarnings("unchecked")
            Map<String, Double> costs = (Map<String, Double>) def.getOrDefault("costs", Map.of());

            boosterDefs.add(new SellBooster(id, mat, rawDisplayName, loreStr, multiplier, duration, cooldown, slot, costs));
        }

        plugin.sendConsole("<green>Loaded " + boosterDefs.size() + " sell booster definitions.</green>");
    }

    private void loadActiveFromDB() {
        // TODO instead of Object[] use SellBooster
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
                        activeBoosters.computeIfAbsent(islandUUID, k -> Collections.synchronizedList(new ArrayList<>())).add(ab);

                        counter++;
                    }

                    cachedBoosterItems.clear();

                    plugin.sendConsole("<green>Loaded " + counter + " active boosters from database.</green>");
                }));
    }

    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            List<ExpiredBoosterInfo> expired = getExpiredBoosterInfos();

            if (!expired.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (ExpiredBoosterInfo info : expired) {
                        cachedBoosterItems.remove(info.islandUUID);
                        String message = Messages.BOOSTER_EXPIRED.toString().replace("%booster%", info.booster.id());

                        if (plugin.getSuperiorSkyblock2Hook() != null) {
                            for (Player p : plugin.getSuperiorSkyblock2Hook().getOnlineIslandMembers(info.islandUUID))
                                plugin.sendMessage(p, message);
                        }
                    }
                });
            }
        }, 600L, 600L);
    }

    private @NonNull List<ExpiredBoosterInfo> getExpiredBoosterInfos() {
        List<ExpiredBoosterInfo> expired = new ArrayList<>();

        for (Map.Entry<String, List<ActiveBooster>> entry : activeBoosters.entrySet()) {
            String islandUUID = entry.getKey();
            List<ActiveBooster> list = entry.getValue();

            synchronized (list) {
                list.removeIf(ab -> {
                    if (ab.isExpired()) {
                        expired.add(new ExpiredBoosterInfo(islandUUID, ab.definition()));
                        plugin.getDatabaseManager().deleteActiveBooster(ab.dbId());
                        return true;
                    }

                    return false;
                });
            }
        }
        return expired;
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

        synchronized (boosters) {
            for (ActiveBooster ab : boosters) {
                if (!ab.isExpired())
                    total += ab.definition().multiplier();
            }
        }

        return Math.max(1.0, total);
    }

    public void activateBooster(Player player, String islandUUID, SellBooster booster) {
        if (isAnyBoosterActive(islandUUID)) {
            ActiveBooster current = findFirstActiveBooster(islandUUID);
            String remaining = current != null
                    ? SellBooster.formatDuration(current.getRemainingDuration() / 1000)
                    : "Unknown";
            plugin.sendMessage(player, Messages.BOOSTER_ONLY_ONE.toString()
                    .replace("%remaining%", remaining));
            return;
        }

        ActiveBooster existing = findActiveByDefinition(islandUUID, booster);
        if (existing != null && existing.isOnCooldown()) {
            plugin.sendMessage(player, Messages.BOOSTER_ON_COOLDOWN.toString()
                    .replace("%remaining%", booster.formatDuration(existing.getRemainingCooldown() / 1000)));
            return;
        }

        if (!booster.costs().isEmpty() && plugin.getExcellentEconomyHook().isEnabled()) {
            for (Map.Entry<String, Double> entry : booster.costs().entrySet()) {
                double balance = plugin.getExcellentEconomyHook().getBalance(player, entry.getKey());

                if (balance < entry.getValue()) {
                    plugin.sendMessage(player, Messages.BOOSTER_NO_MONEY.toString()
                            .replace("%cost%", booster.formatCosts()));
                    return;
                }

                plugin.getExcellentEconomyHook().removeMoney(player, entry.getKey(), entry.getValue());
            }
        }

        long expiryTime = booster.durationSeconds() == -1 ? -1
                : System.currentTimeMillis() + (booster.durationSeconds() * 1000);
        long cooldownEnd = booster.cooldownSeconds() == -1 ? -1
                : System.currentTimeMillis() + (booster.cooldownSeconds() * 1000);

        plugin.getDatabaseManager().saveActiveBooster(islandUUID, booster.id(), expiryTime, cooldownEnd);

        ActiveBooster ab = new ActiveBooster(-1, islandUUID, booster, expiryTime, cooldownEnd);
        activeBoosters.computeIfAbsent(islandUUID, k -> Collections.synchronizedList(new ArrayList<>())).add(ab);

        String durationStr = booster.durationSeconds() == -1
                ? "permanent"
                : booster.formatDuration(booster.durationSeconds());

        plugin.sendMessage(player, Messages.BOOSTER_ACTIVATED.toString()
                .replace("%booster%", booster.id())
                .replace("%multiplier%", String.valueOf(booster.multiplier()))
                .replace("%duration%", durationStr));

        if (plugin.getSuperiorSkyblock2Hook() != null) {
            String broadcastMsg = Messages.BOOSTER_ACTIVATED_BROADCAST.toString()
                    .replace("%booster%", booster.id())
                    .replace("%multiplier%", String.valueOf(booster.multiplier()))
                    .replace("%duration%", durationStr);
            for (Player member : plugin.getSuperiorSkyblock2Hook().getOnlineIslandMembers(islandUUID)) {
                if (!member.equals(player))
                    plugin.sendMessage(member, broadcastMsg);
            }
        }

        cachedBoosterItems.remove(islandUUID);
    }

    public boolean isBoosterActive(String islandUUID, String boosterId) {
        List<ActiveBooster> boosters = activeBoosters.get(islandUUID);

        if (boosters == null)
            return false;

        synchronized (boosters) {
            return boosters.stream()
                    .anyMatch(ab -> ab.definition().id().equals(boosterId) && !ab.isExpired());
        }
    }

    public boolean isAnyBoosterActive(String islandUUID) {
        List<ActiveBooster> boosters = activeBoosters.get(islandUUID);

        if (boosters == null)
            return false;

        synchronized (boosters) {
            return boosters.stream().anyMatch(ab -> !ab.isExpired());
        }
    }

    public ActiveBooster findFirstActiveBooster(String islandUUID) {
        List<ActiveBooster> boosters = activeBoosters.get(islandUUID);
        if (boosters == null) return null;

        synchronized (boosters) {
            for (ActiveBooster ab : boosters) {
                if (!ab.isExpired()) return ab;
            }
        }
        return null;
    }

    private ActiveBooster findActiveByDefinition(String islandUUID, SellBooster booster) {
        List<ActiveBooster> boosters = activeBoosters.get(islandUUID);

        if (boosters == null)
            return null;

        synchronized (boosters) {
            for (ActiveBooster ab : boosters) {
                if (ab.definition().id().equals(booster.id()))
                    return ab;
            }
        }

        return null;
    }

    public void placeBoosterItems(SellPortal portal, Inventory gui) {
        String islandUUID = portal.getIslandUUID();

        Map<Integer, ItemStack> cached = cachedBoosterItems.get(islandUUID);
        if (cached != null) {
            for (Map.Entry<Integer, ItemStack> entry : cached.entrySet())
                gui.setItem(entry.getKey(), entry.getValue().clone());
            return;
        }

        Map<Integer, ItemStack> items = new HashMap<>();
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
            ItemStack clone = item.clone();
            items.put(booster.guiSlot(), clone);
            gui.setItem(booster.guiSlot(), item);
        }
        cachedBoosterItems.put(islandUUID, items);
    }

    public void invalidateBoosterCache(String islandUUID) {
        if (islandUUID != null)
            cachedBoosterItems.remove(islandUUID);
    }

    public void reloadDefinitions() {
        loadDefinitions();

        long now = System.currentTimeMillis();

        for (Map.Entry<String, List<ActiveBooster>> entry : activeBoosters.entrySet()) {
            String islandUUID = entry.getKey();
            List<ActiveBooster> list = entry.getValue();
            synchronized (list) {
                List<ActiveBooster> updated = new ArrayList<>();
                for (ActiveBooster ab : list) {
                    SellBooster newDef = getDefinition(ab.definition().id());
                    if (newDef == null) {
                        updated.add(ab);
                        continue;
                    }

                    long activationTime = (ab.definition().durationSeconds() != -1 && ab.expiryTime() != -1)
                            ? ab.expiryTime() - (ab.definition().durationSeconds() * 1000)
                            : -1;

                    long newExpiry;
                    if (newDef.durationSeconds() == -1) {
                        newExpiry = -1;
                    } else if (activationTime == -1) {
                        newExpiry = now + (newDef.durationSeconds() * 1000);
                    } else {
                        newExpiry = activationTime + (newDef.durationSeconds() * 1000);
                        if (newExpiry <= now) {
                            plugin.getDatabaseManager().deleteActiveBooster(ab.dbId());
                            continue;
                        }
                    }

                    long cooldownStart = (ab.definition().cooldownSeconds() != -1 && ab.cooldownEnd() != -1)
                            ? ab.cooldownEnd() - (ab.definition().cooldownSeconds() * 1000)
                            : -1;

                    long newCooldownEnd;
                    if (newDef.cooldownSeconds() == -1) {
                        newCooldownEnd = -1;
                    } else if (cooldownStart == -1) {
                        newCooldownEnd = now + (newDef.cooldownSeconds() * 1000);
                    } else {
                        newCooldownEnd = cooldownStart + (newDef.cooldownSeconds() * 1000);
                        if (newCooldownEnd <= now)
                            newCooldownEnd = -1;
                    }

                    plugin.getDatabaseManager().deleteActiveBooster(ab.dbId());
                    plugin.getDatabaseManager().saveActiveBooster(islandUUID, newDef.id(), newExpiry, newCooldownEnd);

                    updated.add(new ActiveBooster(ab.dbId(), islandUUID, newDef, newExpiry, newCooldownEnd));
                }
                list.clear();
                list.addAll(updated);
            }
        }

        cachedBoosterItems.clear();

        plugin.sendConsole("<green>Reloaded " + boosterDefs.size() + " booster definitions and updated active boosters.</green>");
    }

    public void shutdown() {
        plugin.getDatabaseManager().flushBoosterOps();
        cachedBoosterItems.clear();
        activeBoosters.clear();
        boosterDefs.clear();
    }

    private record ExpiredBoosterInfo(String islandUUID, SellBooster booster) {
    }
}
