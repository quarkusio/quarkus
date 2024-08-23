package io.quarkus.runtime.configuration;

import java.util.UUID;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 * The initial configuration for Runtime.
 */
public class RuntimeConfigBuilder implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        new QuarkusConfigBuilderCustomizer().configBuilder(builder);
        builder.withDefaultValue("quarkus.uuid", UUID.randomUUID().toString());

        builder.forClassLoader(Thread.currentThread().getContextClassLoader())
                .addDefaultInterceptors()
                .addDefaultSources();
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }
}
