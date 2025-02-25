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
        // Install new config
        if (config != null) {
            QuarkusConfigFactory.config = config;
            // Someone may have called ConfigProvider.getConfig which automatically registers a Config in the TCCL
            configProviderResolver.releaseConfig(Thread.currentThread().getContextClassLoader());
            configProviderResolver.registerConfig(config, Thread.currentThread().getContextClassLoader());
        }
    }

    public static void releaseTCCLConfig() {
        SmallRyeConfigProviderResolver configProviderResolver = (SmallRyeConfigProviderResolver) SmallRyeConfigProviderResolver
                .instance();
        configProviderResolver.releaseConfig(Thread.currentThread().getContextClassLoader());
    }
}
