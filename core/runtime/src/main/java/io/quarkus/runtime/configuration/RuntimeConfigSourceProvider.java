package io.quarkus.runtime.configuration;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class RuntimeConfigSourceProvider implements ConfigSourceProvider {
    private final String configSourceProviderClassName;

    public RuntimeConfigSourceProvider(final String configSourceProviderClassName) {
        this.configSourceProviderClassName = configSourceProviderClassName;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        try {
            Class<ConfigSourceProvider> configSourceProviderClass = (Class<ConfigSourceProvider>) forClassLoader
                    .loadClass(configSourceProviderClassName);
            ConfigSourceProvider configSourceProvider = configSourceProviderClass.getDeclaredConstructor().newInstance();
            return configSourceProvider.getConfigSources(forClassLoader);
        } catch (ClassNotFoundException | InstantiationException | InvocationTargetException | NoSuchMethodException
                | IllegalAccessException e) {
            throw new ConfigurationException(e);
        }
    }
}
