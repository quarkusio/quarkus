package io.quarkus.runtime.configuration;

import io.smallrye.config.ConfigSourceFactory;

public interface ConfigSourceFactoryProvider {
    ConfigSourceFactory getConfigSourceFactory(final ClassLoader forClassLoader);
}
