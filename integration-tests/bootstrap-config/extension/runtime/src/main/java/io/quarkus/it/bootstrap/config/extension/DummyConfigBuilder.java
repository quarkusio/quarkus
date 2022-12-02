package io.quarkus.it.bootstrap.config.extension;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * This would make sure that quarkus.config.locations is only set during runtime (and not used and recorded during
 * build time).
 */
public class DummyConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withDefaultValue("quarkus.config.locations", "config.properties");
        return builder;
    }
}
