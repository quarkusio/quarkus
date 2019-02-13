package org.shamrock.runtime.configuration;

import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 *
 */
public class TestConfigProviderResolver extends ConfigProviderResolver {
    public static volatile Config config;

    public Config getConfig() {
        return config;
    }

    public Config getConfig(final ClassLoader loader) {
        return config;
    }

    public ConfigBuilder getBuilder() {
        return new SmallRyeConfigBuilder();
    }

    public void registerConfig(final Config config, final ClassLoader classLoader) {

    }

    public void releaseConfig(final Config config) {

    }
}
