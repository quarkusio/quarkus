package io.quarkus.devservice.runtime.config;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

// This should live in the devservices/runtime module, but that module doesn't exist, and adding it is a breaking change
public class DevServicesOverrideConfigBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(new DevServicesOverrideConfigSource(LaunchMode.current()));
    }

    @Override
    public int priority() {
        return 10;
    }
}
