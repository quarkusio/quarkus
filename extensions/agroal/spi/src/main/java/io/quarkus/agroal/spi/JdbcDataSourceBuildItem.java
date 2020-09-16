package io.quarkus.agroal.spi;

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

    private final boolean isDefault;

    public JdbcDataSourceBuildItem(String name, String kind, boolean isDefault) {
        this.name = name;
        this.dbKind = kind;
        this.isDefault = isDefault;
    }

    public String getName() {
        return name;
    }

    public String getDbKind() {
        return dbKind;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
