package io.quarkus.deployment.builditem;

import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.wildfly.common.Assert;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Define an additional configuration source which is used at build time.
 */
public final class BuildTimeConfigurationSourceBuildItem extends MultiBuildItem {
    private final Supplier<ConfigSource> configSourceSupplier;

    /**
     * Construct a new instance.
     *
     * @param configSourceSupplier the config source supplier (must not be {@code null})
     */
    public BuildTimeConfigurationSourceBuildItem(final Supplier<ConfigSource> configSourceSupplier) {
        Assert.checkNotNullParam("configSourceSupplier", configSourceSupplier);
        this.configSourceSupplier = configSourceSupplier;
    }

    /**
     * Construct a new instance.
     *
     * @param configSource the config source (must not be {@code null})
     */
    public BuildTimeConfigurationSourceBuildItem(final ConfigSource configSource) {
        this(() -> Assert.checkNotNullParam("configSource", configSource));
    }

    /**
     * Get the config source supplier.
     *
     * @return the config source supplier
     */
    public Supplier<ConfigSource> getConfigSourceSupplier() {
        return configSourceSupplier;
    }
}
