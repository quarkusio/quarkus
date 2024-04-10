package io.quarkus.observability.runtime;

import io.quarkus.observability.devresource.DevResourcesConfigSource;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class DevResourcesConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(new DevResourcesConfigSource());
    }

    @Override
    public int priority() {
        // greater than any default Microprofile ConfigSource
        return 500;
    }
}
