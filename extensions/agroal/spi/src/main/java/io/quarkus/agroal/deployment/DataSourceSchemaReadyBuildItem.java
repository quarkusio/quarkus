package io.quarkus.agroal.deployment;

import java.util.Collection;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item which can be used to order build processors which need a datasource's
 * schema to be ready (which really means that the tables have been created and
 * any migration run on them) for processing
 */
public final class DataSourceSchemaReadyBuildItem extends SimpleBuildItem {

    private final Collection<String> datasourceNames;

    public DataSourceSchemaReadyBuildItem(final Collection<String> datasourceNames) {
        this.datasourceNames = datasourceNames;
    }

    public Collection<String> getDatasourceNames() {
        return this.datasourceNames;
    }
}
