package io.quarkus.agroal.deployment;

import org.jboss.builder.item.SimpleBuildItem;

/**
 * Marker build item indicating the datasource has been fully initialized.
 */
public final class DataSourceInitializedBuildItem extends SimpleBuildItem {

    public DataSourceInitializedBuildItem() {
    }
}
