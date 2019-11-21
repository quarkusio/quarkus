package io.quarkus.agroal.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 */
public final class DataSourceDriverBuildItem extends SimpleBuildItem {
    private final String driver;

    public DataSourceDriverBuildItem(final String driver) {
        this.driver = driver;
    }

    public String getDriver() {
        return driver;
    }
}
