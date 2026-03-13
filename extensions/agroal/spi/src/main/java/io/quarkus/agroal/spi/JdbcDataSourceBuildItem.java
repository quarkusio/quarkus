package io.quarkus.agroal.spi;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item for JDBC datasources.
 * <p>
 * If you inject this build item when recording runtime init template calls, you are guaranteed the datasources configuration
 * has been injected and datasources can be created.
 */
public final class JdbcDataSourceBuildItem extends MultiBuildItem {

    private final String name;

    private final String dbKind;

    private final Optional<String> dbVersion;

    private final boolean transactionIntegrationEnabled;

    private final boolean xaEnabled;

    private final boolean isDefault;

    /**
     * @deprecated Use {@link #JdbcDataSourceBuildItem(String, String, Optional, boolean, boolean, boolean)} instead.
     */
    @Deprecated
    public JdbcDataSourceBuildItem(String name, String kind, Optional<String> dbVersion,
            boolean transactionIntegrationEnabled, boolean isDefault) {
        this(name, kind, dbVersion, transactionIntegrationEnabled, false, isDefault);
    }

    public JdbcDataSourceBuildItem(String name, String kind, Optional<String> dbVersion,
            boolean transactionIntegrationEnabled, boolean xaEnabled, boolean isDefault) {
        this.name = name;
        this.dbKind = kind;
        this.dbVersion = dbVersion;
        this.transactionIntegrationEnabled = transactionIntegrationEnabled;
        this.xaEnabled = xaEnabled;
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

    public boolean isXaEnabled() {
        return xaEnabled;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
