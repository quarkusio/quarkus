package io.quarkus.opentelemetry.runtime.dev;

import java.io.IOException;
import java.net.URL;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;

public class OpenTelemetryDevServicesConfigBuilder implements ConfigBuilder {
    public static final String OPENTELEMETRY_DEVSERVICES_CONFIG = "opentelemetry-devservices-config.properties";

    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withSources(new DevServicesConfigSourceFactory());
        return builder;
    }

    private static class DevServicesConfigSourceFactory extends AbstractLocationConfigSourceLoader
            implements ConfigSourceProvider {
        @Override
        protected String[] getFileExtensions() {
            return new String[] { "properties" };
        }

        @Override
        protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
            return new PropertiesConfigSource(url, ordinal);
        }

        @Override
        public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
            return loadConfigSources(OPENTELEMETRY_DEVSERVICES_CONFIG, Integer.MIN_VALUE + 500);
        }
    }
}
