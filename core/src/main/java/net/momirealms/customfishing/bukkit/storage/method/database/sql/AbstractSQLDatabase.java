/*
 *  Copyright (C) <2024> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customfishing.bukkit.storage.method.database.sql;

import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.storage.data.PlayerData;
import net.momirealms.customfishing.api.storage.user.UserData;
import net.momirealms.customfishing.bukkit.storage.method.AbstractStorage;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An abstract base class for SQL database implementations that handle player data storage.
 */
public abstract class AbstractSQLDatabase extends AbstractStorage {
    protected String tablePrefix;
    protected final SqlConstants constants;

    public AbstractSQLDatabase(BukkitCustomFishingPlugin plugin) {
        super(plugin);
        this.constants = new SqlConstants(getSQLDialectType());
    }

    abstract SQLDialectType getSQLDialectType();

    /**
     * Get a connection to the SQL database.
     *
     * @return A database connection.
     * @throws SQLException If there is an error establishing a connection.
     */
    public abstract Connection getConnection() throws SQLException;

    /**
     * Create tables for storing data if they don't exist in the database.
     */
    public void createTableIfNotExist() {
        try (Connection connection = getConnection()) {
            final String[] databaseSchema = getSchema(getStorageType().name().toLowerCase(Locale.ENGLISH));
            try (Statement statement = connection.createStatement()) {
                for (String tableCreationStatement : databaseSchema) {
                    statement.execute(tableCreationStatement);
                }
            } catch (SQLException e) {
                plugin.getPluginLogger().warn("Failed to create tables", e);
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().warn("Failed to get sql connection", e);
        } catch (IOException e) {
            plugin.getPluginLogger().warn("Failed to get schema resource", e);
        }
    }

    /**
     * Get the SQL schema from a resource file.
     *
     * @param fileName The name of the schema file.
     * @return An array of SQL statements to create tables.
     * @throws IOException If there is an error reading the schema resource.
     */
    private String[] getSchema(@NotNull String fileName) throws IOException {
        return replaceSchemaPlaceholder(new String(Objects.requireNonNull(plugin.getBootstrap().getResource("schema/" + fileName + ".sql"))
               .readAllBytes(), StandardCharsets.UTF_8)).split(";");
    }

    /**
     * Replace placeholder values in SQL schema with the table prefix.
     *
     * @param sql The SQL schema string.
     * @return The SQL schema string with placeholders replaced.
     */
    private String replaceSchemaPlaceholder(@NotNull String sql) {
        return sql.replace("{prefix}", tablePrefix);
    }

    /**
     * Get the name of a database table based on a sub-table name and the table prefix.
     *
     * @param sub The sub-table name.
     * @return The full table name.
     */
    public String getTableName(String sub) {
        return getTablePrefix() + "_" + sub;
    }

    /**
     * Get the current table prefix.
     *
     * @return The table prefix.
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock, Executor executor) {
        var future = new CompletableFuture<Optional<PlayerData>>();
        if (executor == null) executor = plugin.getScheduler().async();
        executor.execute(() -> {
            try (
                    Connection connection = getConnection();
                    PreparedStatement statement = connection.prepareStatement(String.format(constants.sqlSelectByUuid(), getTableName("data")))
            ) {
                statement.setString(1, uuid.toString());
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    final byte[] dataByteArray = rs.getBytes("data");
                    PlayerData data = plugin.getStorageManager().fromBytes(dataByteArray);
                    data.uuid(uuid);
                    if (lock) {
                        int lockValue = rs.getInt(2);
                        if (lockValue != 0 && getCurrentSeconds() - 30 <= lockValue) {
                            connection.close();
                            data.locked(true);
                            future.complete(Optional.of(data));
                            plugin.getPluginLogger().warn("Player " + uuid + "'s data is locked. Retrying...");
                            return;
                        }
                    }
                    if (lock) lockOrUnlockPlayerData(uuid, true);
                    future.complete(Optional.of(data));
                } else if (Bukkit.getPlayer(uuid) != null) {
                    // the player is online
                    var data = PlayerData.empty();
                    data.uuid(uuid);
                    insertPlayerData(uuid, data, lock, connection);
                    future.complete(Optional.of(data));
                } else {
                    future.complete(Optional.empty());
                }
            } catch (SQLException e) {
                plugin.getPluginLogger().warn("Failed to get " + uuid + "'s data.", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean unlock) {
        var future = new CompletableFuture<Boolean>();
        plugin.getScheduler().async().execute(() -> {
        try (
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(String.format(constants.sqlUpdateByUuid(), getTableName("data")))
        ) {
            statement.setInt(1, unlock ? 0 : getCurrentSeconds());
            statement.setBytes(2, playerData.toBytes());
            statement.setString(3, uuid.toString());
            statement.executeUpdate();
            future.complete(true);
        } catch (SQLException e) {
            plugin.getPluginLogger().warn("Failed to update " + uuid + "'s data.", e);
            future.completeExceptionally(e);
        }
        });
        return future;
    }

    @Override
    public void updateManyPlayersData(Collection<? extends UserData> users, boolean unlock) {
        String sql = String.format(constants.sqlUpdateByUuid(), getTableName("data"));
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (UserData user : users) {
                    statement.setInt(1, unlock ? 0 : getCurrentSeconds());
                    statement.setBytes(2, plugin.getStorageManager().toBytes(user.toPlayerData()));
                    statement.setString(3, user.uuid().toString());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                plugin.getPluginLogger().warn("Failed to update data for online players", e);
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().warn("Failed to get connection when saving online players' data", e);
        }
    }

    protected void insertPlayerData(UUID uuid, PlayerData playerData, boolean lock, @Nullable Connection previous) {
        try (
            Connection connection = previous == null ? getConnection() : previous;
            PreparedStatement statement = connection.prepareStatement(String.format(constants.sqlInsertDataByUuid(), getTableName("data")))
        ) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, lock ? getCurrentSeconds() : 0);
            statement.setBytes(3, plugin.getStorageManager().toBytes(playerData));
            statement.execute();
        } catch (SQLException e) {
            plugin.getPluginLogger().warn("Failed to insert " + uuid + "'s data.", e);
        }
    }

    @Override
    public void lockOrUnlockPlayerData(UUID uuid, boolean lock) {
        try (
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(String.format(constants.sqlLockByUuid(), getTableName("data")))
        ) {
            statement.setInt(1, lock ? getCurrentSeconds() : 0);
            statement.setString(2, uuid.toString());
            statement.execute();
        } catch (SQLException e) {
            plugin.getPluginLogger().warn("Failed to lock " + uuid + "'s data.", e);
        }
    }

    @Override
    public CompletableFuture<Boolean> updateOrInsertPlayerData(UUID uuid, PlayerData playerData, boolean unlock) {
        var future = new CompletableFuture<Boolean>();
        plugin.getScheduler().async().execute(() -> {
            try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(String.format(constants.sqlSelectByUuid(), getTableName("data")))
            ) {
                statement.setString(1, uuid.toString());
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    try (
                        PreparedStatement statement2 = connection.prepareStatement(String.format(constants.sqlUpdateByUuid(), getTableName("data")))
                    ) {
                        statement2.setInt(1, unlock ? 0 : getCurrentSeconds());
                        statement2.setBytes(2, plugin.getStorageManager().toBytes(playerData));
                        statement2.setString(3, uuid.toString());
                        statement2.executeUpdate();
                    } catch (SQLException e) {
                        plugin.getPluginLogger().warn("Failed to update " + uuid + "'s data.", e);
                    }
                    future.complete(true);
                } else {
                    insertPlayerData(uuid, playerData, !unlock, connection);
                    future.complete(true);
                }
            } catch (SQLException e) {
                plugin.getPluginLogger().warn("Failed to get " + uuid + "'s data.", e);
            }
        });
        return future;
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        Set<UUID> uuids = new HashSet<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(String.format(constants.sqlSelectAllUuid(), getTableName("data")))) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    uuids.add(uuid);
                }
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().warn("Failed to get unique data.", e);
        }
        return uuids;
    }

    /**
     * Constants defining SQL statements used for database operations.
     */
    public static class SqlConstants {
        private final SQLDialectType dialect;

        public SqlConstants(SQLDialectType dialect) {
            this.dialect = dialect;
        }

        public String sqlSelectByUuid() {
            String q = dialect.getQuote();
            return "SELECT * FROM " + q + "%s" + q + " WHERE " + q + "uuid" + q + " = ?";
        }

        public String sqlSelectAllUuid() {
            String q = dialect.getQuote();
            return "SELECT " + q + "uuid" + q + " FROM " + q + "%s" + q;
        }

        public String sqlUpdateByUuid() {
            String q = dialect.getQuote();
            return "UPDATE " + q + "%s" + q + " SET " + q + "lock" + q + " = ?, " + q + "data" + q + " = ? WHERE " + q + "uuid" + q + " = ?";
        }

        public String sqlLockByUuid() {
            String q = dialect.getQuote();
            return "UPDATE " + q + "%s" + q + " SET " + q + "lock" + q + " = ? WHERE " + q + "uuid" + q + " = ?";
        }

        public String sqlInsertDataByUuid() {
            String q = dialect.getQuote();
            return "INSERT INTO " + q + "%s" + q + "(" + q + "uuid" + q + ", " + q + "lock" + q + ", " + q + "data" + q + ") VALUES(?, ?, ?)";
        }
    }

    public enum SQLDialectType {
        SQLITE("`"),
        POSTGRESQL("\""),
        MYSQL("`"),
        MARIADB("`"),
        H2("`");

        private final String quote;

        SQLDialectType(final String quote) {
            this.quote = quote;
        }

        public String getQuote() {
            return quote;
        }
    }
}
