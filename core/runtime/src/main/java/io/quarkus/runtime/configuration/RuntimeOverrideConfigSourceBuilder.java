package io.quarkus.runtime.configuration;

import io.smallrye.config.SmallRyeConfigBuilder;

public class RuntimeOverrideConfigSourceBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(new RuntimeOverrideConfigSource(builder.getClassLoader()));
    }
}
