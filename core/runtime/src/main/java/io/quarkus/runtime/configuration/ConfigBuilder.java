package io.quarkus.runtime.configuration;

import io.smallrye.config.SmallRyeConfigBuilder;

public interface ConfigBuilder {
    SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder);

    default int priority() {
        return 0;
    }
}
