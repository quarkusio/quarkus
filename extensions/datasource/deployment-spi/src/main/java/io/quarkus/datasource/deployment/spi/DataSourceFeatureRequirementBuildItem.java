package io.quarkus.datasource.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item produced by extensions to declare that a datasource requires
 * a specific database feature.
 * <p>
 * Multiple extensions can produce requirements for the same datasource.
 * <p>
 * DevServices will aggregate all requirements and select an appropriate
 * container image that supports the required features.
 */
public final class DataSourceFeatureRequirementBuildItem extends MultiBuildItem {

    private final String datasourceName;
    private final DatabaseFeature feature;

    public DataSourceFeatureRequirementBuildItem(String datasourceName, DatabaseFeature feature) {
        this.datasourceName = datasourceName;
        this.feature = feature;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public DatabaseFeature getFeature() {
        return feature;
    }
}
