package io.quarkus.devservice.runtime.config;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * @deprecated Subject to changes due to <a href="https://github.com/quarkusio/quarkus/pull/51209">#51209</a>
 */
@Deprecated(forRemoval = true)
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
