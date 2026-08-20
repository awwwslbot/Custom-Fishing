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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.storage.StorageType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public abstract class AbstractHikariDatabase extends AbstractSQLDatabase {

    private HikariDataSource dataSource;
    private final String driverClass;
    private final String sqlBrand;

    public AbstractHikariDatabase(BukkitCustomFishingPlugin plugin) {
        super(plugin);
        final var driverClasses = Objects.requireNonNull(driverClass(getStorageType()));
        sqlBrand = Objects.requireNonNull(sqlBrand(getStorageType()));
        String finalClazz = null;
        for (String clazz : driverClasses) {
            try {
                Class.forName(clazz);
                finalClazz = clazz;
                break;
            } catch (ClassNotFoundException e) {
                plugin.getPluginLogger().info("Driver class " + clazz + " not found.");
            }
        }
        if(finalClazz == null) {
            plugin.getPluginLogger().warn("No" + sqlBrand + "driver is found" );
        }
        driverClass = finalClazz;
    }

    private static List<String> driverClass(final StorageType storageType) {
        switch(storageType) {
            case MariaDB -> {
                return List.of("org.mariadb.jdbc.Driver");
            }
            case MySQL -> {
                return List.of("com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver");
            }
            case PostgreSQL -> {
                return List.of("org.postgresql.Driver");
            }
        }
        return null;
    }
    private static String sqlBrand(final StorageType storageType) {
        switch (storageType) {
            case MariaDB -> {
                return "MariaDB";
            }
            case MySQL -> {
                return "MySQL";
            }
            case PostgreSQL -> {
                return "PostgreSQL";
            }
        }
        return null;
    }

    @Override
    public void initialize(YamlDocument config) {
        Section section = config.getSection(sqlBrand);

        if (section == null) {
            plugin.getPluginLogger().warn("Failed to load database config. It seems that your config is broken. Please regenerate a new one.");
            return;
        }

        super.tablePrefix = section.getString("table-prefix", "customfishing");
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(section.getString("user", "root"));
        hikariConfig.setPassword(section.getString("password", "pa55w0rd"));
        hikariConfig.setJdbcUrl(String.format("jdbc:%s://%s:%s/%s%s",
                sqlBrand.toLowerCase(Locale.ENGLISH),
                section.getString("host", "localhost"),
                section.getString("port", "3306"),
                section.getString("database", "minecraft"),
                section.getString("connection-parameters")
        ));
        hikariConfig.setDriverClassName(driverClass);
        hikariConfig.setMaximumPoolSize(section.getInt("Pool-Settings.max-pool-size", 10));
        hikariConfig.setMinimumIdle(section.getInt("Pool-Settings.min-idle", 10));
        hikariConfig.setMaxLifetime(section.getLong("Pool-Settings.max-lifetime", 180000L));
        hikariConfig.setConnectionTimeout(section.getLong("Pool-Settings.time-out", 20000L));
        hikariConfig.setPoolName("CustomFishingHikariPool");
        try {
            hikariConfig.setKeepaliveTime(section.getLong("Pool-Settings.keep-alive-time", 60000L));
        } catch (NoSuchMethodError ignored) {
        }

        final Properties properties = new Properties();
        properties.putAll(
                Map.of("cachePrepStmts", "true",
                        "prepStmtCacheSize", "250",
                        "prepStmtCacheSqlLimit", "2048",
                        "useServerPrepStmts", "true",
                        "useLocalSessionState", "true",
                        "useLocalTransactionState", "true"
                ));
        properties.putAll(
                Map.of(
                        "rewriteBatchedStatements", "true",
                        "cacheResultSetMetadata", "true",
                        "cacheServerConfiguration", "true",
                        "elideSetAutoCommits", "true",
                        "maintainTimeStats", "false")
        );
        hikariConfig.setDataSourceProperties(properties);
        dataSource = new HikariDataSource(hikariConfig);
        super.createTableIfNotExist();
    }

    @Override
    public void disable() {
        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
