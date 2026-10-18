package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

@StaticInitSafe
public class StaticInitSafeConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {
    public StaticInitSafeConfigBuilderCustomizer() {
        System.out.println("StaticInitSafeConfigBuilderCustomizer.StaticInitSafeConfigBuilderCustomizer");
    }

    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withSources(new StaticInitSafeConfigSource());
    }
}
