package net.momirealms.customfishing.bukkit.storage.method.database.sql;

import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.storage.StorageType;

public class PostgreSQLProvider extends AbstractHikariDatabase {
    public PostgreSQLProvider(BukkitCustomFishingPlugin plugin) {
        super(plugin);
    }

    /**
     * Retrieves the type of storage used by this provider.
     *
     * @return the {@link StorageType} of this provider
     */
    @Override
    public StorageType getStorageType() {
        return StorageType.PostgreSQL;
    }

    @Override
    SQLDialectType getSQLDialectType() {
        return SQLDialectType.POSTGRESQL;
    }
}
