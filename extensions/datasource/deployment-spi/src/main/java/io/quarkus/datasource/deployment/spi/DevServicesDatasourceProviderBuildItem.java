package io.quarkus.datasource.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A provider that knows how to start a database of a specific type.
 */
public final class DevServicesDatasourceProviderBuildItem extends MultiBuildItem {

    private final String database;
    private final DevServicesDatasourceProvider devDBProvider;

    public DevServicesDatasourceProviderBuildItem(String database, DevServicesDatasourceProvider devDBProvider) {
        this.database = database;
        this.devDBProvider = devDBProvider;
    }

    public String getDatabase() {
        return database;
    }

    public DevServicesDatasourceProvider getDevServicesProvider() {
        return devDBProvider;
    }
}
