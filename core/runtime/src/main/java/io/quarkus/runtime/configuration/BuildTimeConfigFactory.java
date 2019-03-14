package io.quarkus.runtime.configuration;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.PropertiesConfigSource;

/**
 *
 */
public final class BuildTimeConfigFactory {

    public static final String BUILD_TIME_CONFIG_NAME = "META-INF/build-config.properties";

    private BuildTimeConfigFactory() {
    }

    public static ConfigSource getBuildTimeConfigSource() {
        Properties properties = new Properties();
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            final Enumeration<URL> resources = classLoader.getResources(BUILD_TIME_CONFIG_NAME);
            if (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                try (InputStream is = url.openStream()) {
                    if (is != null)
                        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                            properties.load(isr);
                        }
                }
            }
            return new PropertiesConfigSource(properties, "Build time configuration");
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
