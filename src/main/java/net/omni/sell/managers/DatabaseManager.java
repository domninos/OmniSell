package net.omni.sell.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.omni.sell.OmniSell;
import net.omni.sell.handlers.SellPortal;
import net.omni.sell.util.ItemSerializationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class DatabaseManager {

    private static final int MAX_OPS_PER_TICK = 20;
    private final OmniSell plugin;
    private final Queue<BoosterOp> pendingOps = new ConcurrentLinkedQueue<>();
    private HikariDataSource dataSource;
    public DatabaseManager(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        File dbFile = new File(plugin.getDataFolder(), "portals.db");

        if (!dbFile.exists()) {
            try {
                if (dbFile.createNewFile())
                    plugin.getLogger().log(Level.INFO, "Successfully created portals.db!");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create portals.db!", e);
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("SellPortalPool");
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(5000);
        config.setLeakDetectionThreshold(2000);
        this.dataSource = new HikariDataSource(config);

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sell_portals (
                        location_key TEXT PRIMARY KEY,
                        owner_uuid TEXT,
                        size INTEGER DEFAULT 3,
                        frame_keys TEXT,
                        island_uuid TEXT,
                        whitelist_base64 TEXT,
                        blacklist_base64 TEXT);
                    """);
            stmt.execute("PRAGMA journal_mode=WAL");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while creating the database!", e);
        }

        initActiveBoostersTable();
        startBoosterQueueProcessor();

        plugin.sendConsole("<green>Successfully initialized database.</green>");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null)
            throw new SQLException("DataSource not initialized");
        return dataSource.getConnection();
    }

    private void initActiveBoostersTable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS active_boosters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        island_uuid TEXT NOT NULL,
                        booster_id TEXT NOT NULL,
                        expiry_time LONG,
                        cooldown_end LONG);
                    """);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while creating active_boosters table!", e);
        }
    }

    private void startBoosterQueueProcessor() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);

                int processed = 0;

                while (processed < MAX_OPS_PER_TICK) {
                    BoosterOp op = pendingOps.poll();

                    if (op == null)
                        break;

                    executeBoosterOp(conn, op);
                    processed++;
                }

                conn.commit();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to process booster queue batch", e);
            }
        }, 600L, 600L);
    }

    private void executeBoosterOp(Connection conn, BoosterOp op) throws SQLException {
        switch (op) {
            case BoosterOp.Save s -> {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO active_boosters (island_uuid, booster_id, expiry_time, cooldown_end) VALUES (?, ?, ?, ?)")) {
                    stmt.setString(1, s.islandUUID());
                    stmt.setString(2, s.boosterId());
                    stmt.setLong(3, s.expiryTime());
                    stmt.setLong(4, s.cooldownEnd());
                    stmt.executeUpdate();
                }
            }

            case BoosterOp.Delete d -> {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM active_boosters WHERE id = ?")) {
                    stmt.setInt(1, d.dbId());
                    stmt.executeUpdate();
                }
            }
        }
    }

    public CompletableFuture<List<ItemStack>> fetchWhitelist(Location location) {
        return fetchColumn(location, "whitelist_base64");
    }

    private CompletableFuture<List<ItemStack>> fetchColumn(Location location, String column) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank())
            return CompletableFuture.completedFuture(List.of());

        CompletableFuture<List<ItemStack>> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String query = "SELECT " + column + " FROM sell_portals WHERE location_key = ?";

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, locationKey);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String base64 = rs.getString(column);
                        if (base64 != null && !base64.isEmpty())
                            future.complete(ItemSerializationUtil.fromBase64(base64));
                        else
                            future.complete(List.of());
                    } else {
                        future.complete(List.of());
                    }
                }
            } catch (SQLException e) {
                future.completeExceptionally(e);
                plugin.getLogger().log(Level.SEVERE, "Error while fetching " + column + " from database!", e);
            }
        });

        return future;
    }

    private String getLocationKey(Location location) {
        if (location == null) return "";
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public CompletableFuture<List<ItemStack>> fetchBlacklist(Location location) {
        return fetchColumn(location, "blacklist_base64");
    }

    public void saveFull(Location location, String ownerUUID, int size,
                         List<String> frameKeys, String islandUUID,
                         List<ItemStack> whitelist, List<ItemStack> blacklist) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank()) return;

        String base64Whitelist = ItemSerializationUtil.toBase64(whitelist);
        String base64Blacklist = ItemSerializationUtil.toBase64(blacklist);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                executeSave(locationKey, ownerUUID, size, frameKeys, islandUUID, base64Whitelist, base64Blacklist));
    }

    private void executeSave(String locationKey, String ownerUUID, int size,
                             List<String> frameKeys, String islandUUID,
                             String base64Whitelist, String base64Blacklist) {
        String query = """
                INSERT OR REPLACE INTO sell_portals
                    (location_key, owner_uuid, size, frame_keys, island_uuid, whitelist_base64, blacklist_base64)
                VALUES (?, ?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, locationKey);
            stmt.setString(2, ownerUUID);
            stmt.setInt(3, size);
            stmt.setString(4, frameKeys.isEmpty() ? null : String.join(",", frameKeys));
            stmt.setString(5, islandUUID);
            stmt.setString(6, base64Whitelist);
            stmt.setString(7, base64Blacklist);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while saving to database!", e);
        }
    }

    public void saveFullSync(Location location, String ownerUUID, int size,
                             List<String> frameKeys, String islandUUID,
                             List<ItemStack> whitelist, List<ItemStack> blacklist) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank()) return;

        String base64Whitelist = ItemSerializationUtil.toBase64(whitelist);
        String base64Blacklist = ItemSerializationUtil.toBase64(blacklist);

        executeSave(locationKey, ownerUUID, size, frameKeys, islandUUID, base64Whitelist, base64Blacklist);
    }

    public SellPortal loadPortalSync(Location location) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank()) return null;

        String query = "SELECT owner_uuid, size, frame_keys, island_uuid, whitelist_base64, blacklist_base64 FROM sell_portals WHERE location_key = ?";

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, locationKey);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String ownerUUID = rs.getString("owner_uuid");
                    int size = rs.getInt("size");

                    String frameKeysStr = rs.getString("frame_keys");
                    List<String> frameKeys = (frameKeysStr != null && !frameKeysStr.isEmpty())
                            ? List.of(frameKeysStr.split(",")) : List.of();

                    String islandUUID = rs.getString("island_uuid");

                    String whitelistB64 = rs.getString("whitelist_base64");
                    String blacklistB64 = rs.getString("blacklist_base64");

                    List<ItemStack> whitelist = (whitelistB64 != null && !whitelistB64.isEmpty())
                            ? ItemSerializationUtil.fromBase64(whitelistB64) : List.of();
                    List<ItemStack> blacklist = (blacklistB64 != null && !blacklistB64.isEmpty())
                            ? ItemSerializationUtil.fromBase64(blacklistB64) : List.of();

                    SellPortal portal = new SellPortal(location, UUID.fromString(ownerUUID), size, islandUUID, plugin);
                    portal.setFrameKeys(frameKeys);
                    portal.loadItems(whitelist, blacklist);
                    return portal;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while loading portal from database!", e);
        }

        return null;
    }

    public int countPortalsSync(UUID ownerUUID) {
        String query = "SELECT COUNT(*) FROM sell_portals WHERE owner_uuid = ?";

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, ownerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while counting portals in database!", e);
        }

        return 0;
    }

    public boolean hasPortalForIslandSync(String islandUUID) {
        if (islandUUID == null || islandUUID.isEmpty()) return false;

        String query = "SELECT 1 FROM sell_portals WHERE island_uuid = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, islandUUID);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while checking island portal in database!", e);
        }

        return false;
    }

    public CompletableFuture<List<SellPortal>> loadAllPortalsAsync() {
        CompletableFuture<List<SellPortal>> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<SellPortal> portals = new ArrayList<>();
            String query = "SELECT location_key, owner_uuid, size, frame_keys, island_uuid, whitelist_base64, blacklist_base64 FROM sell_portals";

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String locationKey = rs.getString("location_key");
                    Location location = locationFromKey(locationKey);
                    if (location == null) continue;

                    String ownerUUID = rs.getString("owner_uuid");
                    int size = rs.getInt("size");

                    String frameKeysStr = rs.getString("frame_keys");
                    List<String> frameKeys = (frameKeysStr != null && !frameKeysStr.isEmpty())
                            ? List.of(frameKeysStr.split(",")) : List.of();

                    String islandUUID = rs.getString("island_uuid");

                    String whitelistB64 = rs.getString("whitelist_base64");
                    String blacklistB64 = rs.getString("blacklist_base64");

                    List<ItemStack> whitelist = (whitelistB64 != null && !whitelistB64.isEmpty())
                            ? ItemSerializationUtil.fromBase64(whitelistB64) : List.of();
                    List<ItemStack> blacklist = (blacklistB64 != null && !blacklistB64.isEmpty())
                            ? ItemSerializationUtil.fromBase64(blacklistB64) : List.of();

                    SellPortal portal = new SellPortal(location, UUID.fromString(ownerUUID), size, islandUUID, plugin);
                    portal.setFrameKeys(frameKeys);
                    portal.loadItems(whitelist, blacklist);
                    portals.add(portal);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while loading all portals from database!", e);
                future.completeExceptionally(e);
                return;
            }

            future.complete(portals);
        });

        return future;
    }

    private Location locationFromKey(String key) {
        String[] parts = key.split(":");
        if (parts.length < 4) return null;
        return new Location(
                plugin.getServer().getWorld(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]));
    }

    public void deleteLocation(Location location) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> executeDelete(locationKey));
    }

    private void executeDelete(String locationKey) {
        String query = "DELETE FROM sell_portals WHERE location_key = ?";

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, locationKey);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while deleting from database!", e);
        }
    }

    public void deleteLocationSync(Location location) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank()) return;

        executeDelete(locationKey);
    }

    public String fetchOwnerSync(Location location) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank()) return null;

        String query = "SELECT owner_uuid FROM sell_portals WHERE location_key = ?";

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, locationKey);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getString("owner_uuid");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while fetching owner from database!", e);
        }

        return null;
    }

    public void saveActiveBooster(String islandUUID, String boosterId, long expiryTime, long cooldownEnd) {
        pendingOps.add(new BoosterOp.Save(islandUUID, boosterId, expiryTime, cooldownEnd));
    }

    public CompletableFuture<List<Object[]>> loadActiveBoostersAsync() {
        CompletableFuture<List<Object[]>> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Object[]> rows = new ArrayList<>();
            String query = "SELECT id, island_uuid, booster_id, expiry_time, cooldown_end FROM active_boosters";

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("id"),
                            rs.getString("island_uuid"),
                            rs.getString("booster_id"),
                            rs.getLong("expiry_time"),
                            rs.getLong("cooldown_end")
                    });
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while loading active boosters!", e);
                future.completeExceptionally(e);
                return;
            }

            future.complete(rows);
        });

        return future;
    }

    public void deleteActiveBooster(int dbId) {
        pendingOps.add(new BoosterOp.Delete(dbId));
    }

    public void closePool() {
        flushBoosterOps();

        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();
    }

    public void flushBoosterOps() {
        while (!pendingOps.isEmpty()) {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);

                int processed = 0;

                while (processed < MAX_OPS_PER_TICK) {
                    BoosterOp op = pendingOps.poll();

                    if (op == null)
                        break;

                    executeBoosterOp(conn, op);
                    processed++;
                }

                conn.commit();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to drain booster ops", e);
                break;
            }
        }
    }

    private sealed interface BoosterOp {
        record Save(String islandUUID, String boosterId, long expiryTime, long cooldownEnd) implements BoosterOp {
        }

        record Delete(int dbId) implements BoosterOp {
        }
    }
}
