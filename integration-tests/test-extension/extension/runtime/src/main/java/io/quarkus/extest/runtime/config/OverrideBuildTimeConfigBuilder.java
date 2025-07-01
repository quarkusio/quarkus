package io.quarkus.extest.runtime.config;

import java.util.Map;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

public class OverrideBuildTimeConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(
                new MapBackedConfigSource("Override Build Time", Map.of("quarkus.mapping.btrt.value", "override")) {
                });
    }
}
