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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final OmniSell plugin;
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
                        whitelist_base64 TEXT,
                        blacklist_base64 TEXT);
                    """);
            stmt.execute("PRAGMA journal_mode=WAL");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while creating the database!", e);
        }

        plugin.sendConsole("<green>Successfully initialized database.</green>");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null)
            throw new SQLException("DataSource not initialized");
        return dataSource.getConnection();
    }

    private String getLocationKey(Location location) {
        if (location == null) return "";
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public CompletableFuture<List<ItemStack>> fetchWhitelist(Location location) {
        return fetchColumn(location, "whitelist_base64");
    }

    public CompletableFuture<List<ItemStack>> fetchBlacklist(Location location) {
        return fetchColumn(location, "blacklist_base64");
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

    public void saveFull(Location location, String ownerUUID, int size,
                         List<ItemStack> whitelist, List<ItemStack> blacklist) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank()) return;

        String base64Whitelist = ItemSerializationUtil.toBase64(whitelist);
        String base64Blacklist = ItemSerializationUtil.toBase64(blacklist);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                executeSave(locationKey, ownerUUID, size, base64Whitelist, base64Blacklist));
    }

    public void saveFullSync(Location location, String ownerUUID, int size,
                             List<ItemStack> whitelist, List<ItemStack> blacklist) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank()) return;

        String base64Whitelist = ItemSerializationUtil.toBase64(whitelist);
        String base64Blacklist = ItemSerializationUtil.toBase64(blacklist);

        executeSave(locationKey, ownerUUID, size, base64Whitelist, base64Blacklist);
    }

    private void executeSave(String locationKey, String ownerUUID, int size,
                             String base64Whitelist, String base64Blacklist) {
        String query = """
                INSERT OR REPLACE INTO sell_portals
                    (location_key, owner_uuid, size, whitelist_base64, blacklist_base64)
                VALUES (?, ?, ?, ?, ?);
                """;

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, locationKey);
            stmt.setString(2, ownerUUID);
            stmt.setInt(3, size);
            stmt.setString(4, base64Whitelist);
            stmt.setString(5, base64Blacklist);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while saving to database!", e);
        }
    }

    public SellPortal loadPortalSync(Location location) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank()) return null;

        String query = "SELECT owner_uuid, size, whitelist_base64, blacklist_base64 FROM sell_portals WHERE location_key = ?";

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, locationKey);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String ownerUUID = rs.getString("owner_uuid");
                    int size = rs.getInt("size");

                    String whitelistB64 = rs.getString("whitelist_base64");
                    String blacklistB64 = rs.getString("blacklist_base64");

                    List<ItemStack> whitelist = (whitelistB64 != null && !whitelistB64.isEmpty())
                            ? ItemSerializationUtil.fromBase64(whitelistB64) : List.of();
                    List<ItemStack> blacklist = (blacklistB64 != null && !blacklistB64.isEmpty())
                            ? ItemSerializationUtil.fromBase64(blacklistB64) : List.of();

                    SellPortal portal = new SellPortal(location, UUID.fromString(ownerUUID), size, plugin);
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

    public void deleteLocation(Location location) {
        String locationKey = getLocationKey(location);
        if (locationKey.isBlank()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> executeDelete(locationKey));
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

    public void closePool() {
        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();
    }
}
