package io.quarkus.runtime.configuration;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * A simple configuration provider.
 */
public class SimpleConfigurationProviderResolver extends ConfigProviderResolver {

    private static final AtomicReference<Config> CONFIG = new AtomicReference<>();

    public Config getConfig() {
        return CONFIG.get();
    }

    public Config getConfig(final ClassLoader loader) {
        return getConfig();
    }

    public ConfigBuilder getBuilder() {
        return new SmallRyeConfigBuilder();
    }

    public void registerConfig(final Config config, final ClassLoader classLoader) {
        CONFIG.set(config);
    }

    public void releaseConfig(final Config config) {
        CONFIG.set(null);
    }
}
