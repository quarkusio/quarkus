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

    @Override
    public SmallRyeConfig getConfigFor(final SmallRyeConfigProviderResolver configProviderResolver,
            final ClassLoader classLoader) {
        if (config == null) {
            // Remember the config so that we can uninstall it when "setConfig" is next called
            config = ConfigUtils.configBuilder(true, true, LaunchMode.NORMAL).build();
        }
        return config;
    }

    public static void setConfig(SmallRyeConfig config) {
        SmallRyeConfigProviderResolver configProviderResolver = (SmallRyeConfigProviderResolver) SmallRyeConfigProviderResolver
                .instance();
        // Uninstall previous config
        if (QuarkusConfigFactory.config != null) {
            configProviderResolver.releaseConfig(QuarkusConfigFactory.config);
            QuarkusConfigFactory.config = null;
        }
        // Also release the TCCL config, in case that config was not QuarkusConfigFactory.config
        configProviderResolver.releaseConfig(Thread.currentThread().getContextClassLoader());
        // Install new config
        if (config != null) {
            QuarkusConfigFactory.config = config;
            // Register the new config for the TCCL,
            // just in case the TCCL was using a different config
            // than the one we uninstalled above.
            configProviderResolver.registerConfig(config, Thread.currentThread().getContextClassLoader());
        }
    }

    public static void releaseTCCLConfig() {
        SmallRyeConfigProviderResolver configProviderResolver = (SmallRyeConfigProviderResolver) SmallRyeConfigProviderResolver
                .instance();
        configProviderResolver.releaseConfig(Thread.currentThread().getContextClassLoader());
    }
}
