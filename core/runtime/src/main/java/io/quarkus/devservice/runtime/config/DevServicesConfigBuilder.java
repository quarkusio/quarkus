package io.quarkus.devservice.runtime.config;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

// This should live in the devservices/runtime module, but that module doesn't exist, and adding it is a breaking change
public class DevServicesConfigBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(new DevServicesConfigSource(LaunchMode.current()));
    }

    @Override
    public int priority() {
        // What's the right priority? This is a cheeky dynamic override, so a high priority seems correct, but dev services are supposed to fill in gaps in existing information.
        // Dev services should be looking at those sources and not doing anything if there's existing config,
        // so a very low priority is also arguably correct.
        // In principle the priority actually shouldn't matter much, but in practice it needs to not be higher than Arquillian config overrides or some tests fail

        return 10;
    }
}
