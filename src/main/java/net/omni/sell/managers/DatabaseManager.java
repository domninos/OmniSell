package net.omni.sell.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.omni.sell.OmniSell;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final OmniSell plugin;

    private HikariDataSource dataSource;

    public DatabaseManager(OmniSell plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        File dbFile = new File(plugin.getDataFolder(), "hoppers.db");

        if (!dbFile.exists()) {
            try {
                if (dbFile.createNewFile())
                    plugin.getLogger().log(Level.INFO, "Successfully created hoppers.db!");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create hoppers.db!", e);
            }
        }

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("ChunkHopperPool");
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(5000); // 5 seconds timeout
        config.setLeakDetectionThreshold(2000); // checks if a connection is bleeding

        this.dataSource = new HikariDataSource(config);

        // create table with all columns
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS chunk_hoppers (
                        location_key TEXT PRIMARY KEY,
                        owner_uuid TEXT,
                        base64_items TEXT,
                        whitelist_base64 TEXT,
                        blacklist_base64 TEXT);
                    """);
            stmt.execute("PRAGMA journal_mode=WAL");
            migrateSchema(conn);
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

    private void migrateSchema(Connection conn) {
        String[] newColumns = {"whitelist_base64", "blacklist_base64"};
        for (String col : newColumns) {
            String sql = "ALTER TABLE chunk_hoppers ADD COLUMN " + col + " TEXT;";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                plugin.sendConsole("<green>Migrated database.</green>");
            } catch (SQLException ignored) {
                // column likely already exists
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
            String query = "SELECT " + column + " FROM chunk_hoppers WHERE location_key = ?";

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, locationKey);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String base64 = rs.getString(column);
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
        if (location == null)
            return "";

        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public CompletableFuture<List<ItemStack>> fetchBlacklist(Location location) {
        return fetchColumn(location, "blacklist_base64");
    }

    public void saveFull(Location location, String ownerUUID, List<ItemStack> items,
                         List<ItemStack> whitelist, List<ItemStack> blacklist) {
        String locationKey = getLocationKey(location);

        if (locationKey.isBlank()) {
        }

    }

    private void executeSave(String locationKey, String ownerUUID,
                             String base64Items, String base64Whitelist,
                             String base64Blacklist) {
        String query = """
                INSERT OR REPLACE INTO chunk_hoppers
                    (location_key, owner_uuid, base64_items, whitelist_base64, blacklist_base64)
                VALUES (?, ?, ?, ?, ?);
                """;

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, locationKey);
            stmt.setString(2, ownerUUID);
            stmt.setString(3, base64Items);
            stmt.setString(4, base64Whitelist);
            stmt.setString(5, base64Blacklist);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while saving to database!", e);
        }
    }

    public void saveFullSync(Location location, String ownerUUID, List<ItemStack> items,
                             List<ItemStack> whitelist, List<ItemStack> blacklist) {
        String locationKey = getLocationKey(location);

        if (locationKey.isBlank()) {
        }
    }

    public void saveAsync(Location location, List<ItemStack> items) {
        String locationKey = getLocationKey(location);

        if (locationKey.isBlank())
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String query = "INSERT OR REPLACE INTO chunk_hoppers (location_key, base64_items) VALUES (?, ?);";

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, locationKey);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while saving the database!", e);
            }
        });
    }

    private void executeDelete(String locationKey) {
        String query = "DELETE FROM chunk_hoppers WHERE location_key = ?";

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
