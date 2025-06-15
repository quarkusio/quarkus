package io.quarkus.agroal.spi;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item for JDBC datasources.
 * <p>
 * If you inject this build item when recording runtime init template calls, you are guaranteed the datasources
 * configuration has been injected and datasources can be created.
 */
public final class JdbcDataSourceBuildItem extends MultiBuildItem {

    private final String name;

    private final String dbKind;

    private final Optional<String> dbVersion;

    private final boolean transactionIntegrationEnabled;

    private final boolean isDefault;

    public JdbcDataSourceBuildItem(String name, String kind, Optional<String> dbVersion,
            boolean transactionIntegrationEnabled, boolean isDefault) {
        this.name = name;
        this.dbKind = kind;
        this.dbVersion = dbVersion;
        this.transactionIntegrationEnabled = transactionIntegrationEnabled;
        this.isDefault = isDefault;
    }

    public String getName() {
        return name;
    }

    public String getDbKind() {
        return dbKind;
    }

    public Optional<String> getDbVersion() {
        return dbVersion;
    }

    public boolean isTransactionIntegrationEnabled() {
        return transactionIntegrationEnabled;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
