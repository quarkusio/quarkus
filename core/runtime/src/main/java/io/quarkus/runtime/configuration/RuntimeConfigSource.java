package io.quarkus.runtime.configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class RuntimeConfigSource implements ConfigSourceProvider {
    private final String configSourceClassName;

    public RuntimeConfigSource(final String configSourceClassName) {
        this.configSourceClassName = configSourceClassName;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        try {
            Class<ConfigSource> configSourceClass = (Class<ConfigSource>) forClassLoader.loadClass(configSourceClassName);
            ConfigSource configSource = configSourceClass.getDeclaredConstructor().newInstance();
            return Collections.singleton(configSource);
        } catch (ClassNotFoundException | InstantiationException | InvocationTargetException | NoSuchMethodException
                | IllegalAccessException e) {
            throw new ConfigurationException(e);
        }
    }
}
