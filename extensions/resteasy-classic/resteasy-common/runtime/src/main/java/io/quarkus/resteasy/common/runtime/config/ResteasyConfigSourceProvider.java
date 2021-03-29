package io.quarkus.resteasy.common.runtime.config;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.resteasy.microprofile.config.FilterConfigSource;
import org.jboss.resteasy.microprofile.config.ServletConfigSource;
import org.jboss.resteasy.microprofile.config.ServletContextConfigSource;

public class ResteasyConfigSourceProvider implements ConfigSourceProvider {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        List<ConfigSource> configSources = new ArrayList<>();
        configSources.add(new ServletConfigSource());
        configSources.add(new FilterConfigSource());
        configSources.add(new ServletContextConfigSource());
        return configSources;
    }
}
