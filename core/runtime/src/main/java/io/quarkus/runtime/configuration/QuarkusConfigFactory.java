package io.quarkus.runtime.configuration;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigFactory;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * The simple Quarkus implementation of {@link SmallRyeConfigFactory}.
 */
public final class QuarkusConfigFactory extends SmallRyeConfigFactory {

    private static volatile SmallRyeConfig config;

    /**
     * Construct a new instance. Called by service loader.
     */
    public QuarkusConfigFactory() {
        // todo: replace with {@code provider()} post-Java 11
    }

    public SmallRyeConfig getConfigFor(final SmallRyeConfigProviderResolver configProviderResolver,
            final ClassLoader classLoader) {
        return config;
    }

    public static void setConfig(SmallRyeConfig config) {
        QuarkusConfigFactory.config = config;
    }
}
