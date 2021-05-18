package io.quarkus.runtime.configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.PropertiesConfigSource;

public class ApplicationPropertiesConfigSourceLoader extends AbstractLocationConfigSourceLoader {
    @Override
    protected String[] getFileExtensions() {
        return new String[] { "properties" };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new PropertiesConfigSource(url, ordinal);
    }

    public static class InClassPath extends ApplicationPropertiesConfigSourceLoader implements ConfigSourceProvider {
        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            return loadConfigSources("application.properties", 250, classLoader);
        }

        @Override
        protected List<ConfigSource> tryFileSystem(final URI uri, final int ordinal) {
            return new ArrayList<>();
        }
    }

    public static class InFileSystem extends ApplicationPropertiesConfigSourceLoader implements ConfigSourceProvider {
        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            return loadConfigSources(
                    Paths.get(System.getProperty("user.dir"), "config", "application.properties").toUri().toString(), 260,
                    classLoader);
        }

        @Override
        protected List<ConfigSource> tryClassPath(final URI uri, final int ordinal, final ClassLoader classLoader) {
            return new ArrayList<>();
        }
    }
}
