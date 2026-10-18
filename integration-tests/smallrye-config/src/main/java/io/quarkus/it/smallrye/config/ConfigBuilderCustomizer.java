package io.quarkus.it.smallrye.config;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

@StaticInitSafe
public class ConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withDefaultValue("exception.message", "This is an exception!");
    }
}
