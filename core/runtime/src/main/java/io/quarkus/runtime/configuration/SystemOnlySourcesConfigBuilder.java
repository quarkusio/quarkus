package io.quarkus.runtime.configuration;

import io.smallrye.config.SmallRyeConfigBuilder;

public class SystemOnlySourcesConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.setAddDefaultSources(false).addSystemSources();
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }
}
