package io.quarkus.datasource.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A provider that knows how to start a database of a specific type.
 */
public final class DevServicesDatasourceProviderBuildItem extends MultiBuildItem {

    private final String database;
    private final DevServicesDatasourceProvider devDBProvider;
    private final DeferredDevServicesDatasourceProvider deferredDevServicesDatasourceProvider;

    @Deprecated(since = "3.32", forRemoval = true)
    public DevServicesDatasourceProviderBuildItem(String database, DevServicesDatasourceProvider devDBProvider) {
        this.database = database;
        this.devDBProvider = devDBProvider;
        this.deferredDevServicesDatasourceProvider = null;
    }

    public DevServicesDatasourceProviderBuildItem(String database,
            DeferredDevServicesDatasourceProvider deferredDevServicesDatasourceProvider) {
        this.database = database;
        this.deferredDevServicesDatasourceProvider = deferredDevServicesDatasourceProvider;
        this.devDBProvider = null;
    }

    public String getDatabase() {
        return database;
    }

    /**
     * May be null if the new-model dev services API is being used
     *
     */
    @Deprecated(since = "3.32", forRemoval = true)
    public DevServicesDatasourceProvider getDevServicesProvider() {
        return devDBProvider;
    }

    /**
     * May be null if the old-model dev services API is being used
     *
     */
    public DeferredDevServicesDatasourceProvider getDeferredDevServicesProvider() {
        return deferredDevServicesDatasourceProvider;
    }

}
