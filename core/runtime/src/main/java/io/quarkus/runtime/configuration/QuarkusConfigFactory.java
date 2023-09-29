package io.quarkus.runtime.configuration;

import io.quarkus.runtime.LaunchMode;
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
        if (config == null) {
            return ConfigUtils.configBuilder(true, true, LaunchMode.NORMAL).build();
        }
        return config;
    }

    public static void setConfig(SmallRyeConfig config) {
        SmallRyeConfigProviderResolver configProviderResolver = (SmallRyeConfigProviderResolver) SmallRyeConfigProviderResolver
                .instance();
        configProviderResolver.releaseConfig(Thread.currentThread().getContextClassLoader());
        QuarkusConfigFactory.config = config;
        if (QuarkusConfigFactory.config != null) {
            configProviderResolver.registerConfig(config, Thread.currentThread().getContextClassLoader());
        }
    }
}
