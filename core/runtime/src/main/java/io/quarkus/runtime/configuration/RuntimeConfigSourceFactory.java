package io.quarkus.runtime.configuration;

import java.lang.reflect.InvocationTargetException;

import io.smallrye.config.ConfigSourceFactory;

public class RuntimeConfigSourceFactory implements ConfigSourceFactoryProvider {
    private final String configSourceFactoryClassName;

    public RuntimeConfigSourceFactory(final String configSourceFactoryClassName) {
        this.configSourceFactoryClassName = configSourceFactoryClassName;
    }

    @Override
    public ConfigSourceFactory getConfigSourceFactory(final ClassLoader forClassLoader) {
        try {
            Class<ConfigSourceFactory> configSourceFactoryClass = (Class<ConfigSourceFactory>) forClassLoader
                    .loadClass(configSourceFactoryClassName);
            return configSourceFactoryClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | InvocationTargetException | NoSuchMethodException
                | IllegalAccessException e) {
            throw new ConfigurationException(e);
        }
    }
}
