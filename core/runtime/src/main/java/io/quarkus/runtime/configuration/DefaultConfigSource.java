package io.quarkus.runtime.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import io.smallrye.config.PropertiesConfigSource;

/**
 * The default values run time configuration source.
 */
public final class DefaultConfigSource extends PropertiesConfigSource {
    private static final long serialVersionUID = -6482737535291300045L;

    public static final String DEFAULT_CONFIG_PROPERTIES_NAME = "META-INF/quarkus-default-config.properties";

    /**
     * Construct a new instance.
     */
    public DefaultConfigSource() {
        super(getMap(), "Default configuration values", 0);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getMap() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = DefaultConfigSource.class.getClassLoader();
        }
        try {
            final Properties p = new Properties();
            // work around #1477
            final Enumeration<URL> resources = cl == null ? ClassLoader.getSystemResources(DEFAULT_CONFIG_PROPERTIES_NAME)
                    : cl.getResources(DEFAULT_CONFIG_PROPERTIES_NAME);
            if (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                try (InputStream is = url.openStream()) {
                    try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        p.load(isr);
                    }
                }
            }
            return (Map) p;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read default configuration", e);
        }
    }
}
