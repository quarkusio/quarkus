package io.quarkus.runtime.configuration;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * A simple configuration provider.
 */
public class SimpleConfigurationProviderResolver extends ConfigProviderResolver {
    private final Config config;

    public SimpleConfigurationProviderResolver(final Config config) {
        this.config = config;
    }

    public Config getConfig() {
        return config;
    }

    public Config getConfig(final ClassLoader loader) {
        return getConfig();
    }

    public ConfigBuilder getBuilder() {
        return new SmallRyeConfigBuilder();
    }

    public void registerConfig(final Config config, final ClassLoader classLoader) {
        // ignore
    }

    public void releaseConfig(final Config config) {
        // ignore
    }
}
