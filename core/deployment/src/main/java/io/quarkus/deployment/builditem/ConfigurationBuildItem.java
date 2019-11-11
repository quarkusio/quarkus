package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;

/**
 * The build item which carries the build time configuration.
 */
public final class ConfigurationBuildItem extends SimpleBuildItem {
    private final BuildTimeConfigurationReader.ReadResult readResult;

    public ConfigurationBuildItem(final BuildTimeConfigurationReader.ReadResult readResult) {
        this.readResult = readResult;
    }

    public BuildTimeConfigurationReader.ReadResult getReadResult() {
        return readResult;
    }
}
