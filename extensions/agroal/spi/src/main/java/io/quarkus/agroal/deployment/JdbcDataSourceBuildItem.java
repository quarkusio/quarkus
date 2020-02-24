package io.quarkus.agroal.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item for JDBC datasources.
 * <p>
 * If you inject this build item when recording runtime init template calls, you are guaranteed the datasources configuration
 * has been injected and datasources can be created.
 */
public final class JdbcDataSourceBuildItem extends MultiBuildItem {

    private final String name;

    private final String driver;

    private final boolean isDefault;

    public JdbcDataSourceBuildItem(String name, String driver, boolean isDefault) {
        this.name = name;
        this.driver = driver;
        this.isDefault = isDefault;
    }

    public String getName() {
        return name;
    }

    public String getDriver() {
        return driver;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
