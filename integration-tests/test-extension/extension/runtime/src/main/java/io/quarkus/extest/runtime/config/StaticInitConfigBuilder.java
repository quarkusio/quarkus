package io.quarkus.extest.runtime.config;

import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class StaticInitConfigBuilder implements ConfigBuilder {
    public static AtomicInteger counter = new AtomicInteger(0);

    public StaticInitConfigBuilder() {
        counter.incrementAndGet();
    }

    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withDefaultValue("skip.build.sources", "true");
        return builder;
    }
}
