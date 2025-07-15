package io.quarkus.runtime.configuration;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.SmallRyeConfigBuilder;

public class RuntimeOverrideConfigSourceBuilder implements ConfigBuilder {
    private static final ConfigSource RUNTIME_OVERRIDE_CONFIG_SOURCE = new RuntimeOverrideConfigSource(
            Thread.currentThread().getContextClassLoader());

    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(RUNTIME_OVERRIDE_CONFIG_SOURCE);
    }
}
