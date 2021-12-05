package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class RunTimeConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withDefaultValue("additional.builder.property", "1234");
        return builder;
    }
}
