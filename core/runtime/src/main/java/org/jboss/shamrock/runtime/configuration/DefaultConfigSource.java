package org.jboss.shamrock.runtime.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import io.smallrye.config.PropertiesConfigSource;

/**
 * The default values run time configuration source.
 */
public final class DefaultConfigSource extends PropertiesConfigSource {
    private static final long serialVersionUID = - 6482737535291300045L;

    public static final String DEFAULT_CONFIG_PROPERTIES_NAME = "META-INF/shamrock-default-config.properties";

    /**
     * Construct a new instance.
     */
    public DefaultConfigSource() {
        super(getMap(), "Default configuration values", 0);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getMap() {
        try (InputStream is = DefaultConfigSource.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PROPERTIES_NAME)) {
            if (is == null) {
                return Collections.emptyMap();
            }
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                final Properties p = new Properties();
                p.load(isr);
                return (Map)p;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read default configuration", e);
        }
    }
}
