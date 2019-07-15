package io.quarkus.axon.runtime;

import io.quarkus.builder.item.SimpleBuildItem;
import org.axonframework.config.Configuration;

public class AxonConfigBuildItem extends SimpleBuildItem {
    private final Configuration Configuration;

    public AxonConfigBuildItem(final Configuration configuration) {
        this.Configuration = configuration;
    }

    public Configuration getConfiguration() {
        return Configuration;
    }
}
