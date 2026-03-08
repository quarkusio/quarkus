package io.quarkus.quickcli.runtime;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class QuickCliConfigSourceBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(new QuickCliConfigSource());
    }
}
