package io.quarkus.runtime.configuration;

import java.util.Arrays;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 * This is a temporary hack until the class loader mess is worked out.
 */
public class TemporaryConfigSourceProvider implements ConfigSourceProvider {
    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        return Arrays.asList(
                new ApplicationPropertiesConfigSource.InJar(),
                new ApplicationPropertiesConfigSource.InFileSystem());
    }
}
