package io.quarkus.runtime.configuration;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

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
            //TODO: this code path is only hit when start fails in dev mode very early in the process
            //the recovery code will fail without this as it cannot read any properties such as
            //the HTTP port or logging info
            return configProviderResolver.getBuilder().forClassLoader(classLoader)
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDiscoveredConverters()
                    .build();
        }
        return config;
    }

    public static void setConfig(SmallRyeConfig config) {
        //This is a hack, but there is not a great deal that can be done about it
        //it is possible that when running in test/dev mode some code will be run with the system TCCL
        //e.g. when using the fork join pool
        //if this happens any use of config will fail, as the system class loader will resolve the
        //wrong instance of the config classes
        if (QuarkusConfigFactory.config != null) {
            ConfigProviderResolver.instance().releaseConfig(QuarkusConfigFactory.config);
        }
        if (config != null) {
            ConfigProviderResolver.instance().registerConfig(config, ClassLoader.getSystemClassLoader());
        }
        QuarkusConfigFactory.config = config;
    }
}
