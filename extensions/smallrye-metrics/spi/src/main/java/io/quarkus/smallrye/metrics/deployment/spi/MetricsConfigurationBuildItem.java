package io.quarkus.smallrye.metrics.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that can be used by build steps that need to know the metrics configuration
 */
@Deprecated
public final class MetricsConfigurationBuildItem extends SimpleBuildItem {

    private final String path;

    public MetricsConfigurationBuildItem(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
