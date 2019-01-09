package org.jboss.shamrock.deployment.logging;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class DefaultLoggingConfigSourceProvider implements ConfigSourceProvider {

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        try {
            Map<String, String> config = new HashMap<>();
            Enumeration<URL> defaultLogging = forClassLoader.getResources("META-INF/shamrock-default-logging.properties");
            while (defaultLogging.hasMoreElements()) {
                URL entry = defaultLogging.nextElement();
                Properties properties = new Properties();
                try (InputStream in = entry.openStream()) {
                    properties.load(in);
                }

                for (Map.Entry<Object, Object> i : properties.entrySet()) {
                    config.put("shamrock.log.category." + i.getKey().toString() + ".level", i.getValue().toString());
                }
            }

            ConfigSource c = new ConfigSource() {
                @Override
                public Map<String, String> getProperties() {
                    return config;
                }

                @Override
                public String getValue(String propertyName) {
                    return config.get(propertyName);
                }

                @Override
                public String getName() {
                    return "Default Log Level Config";
                }

                @Override
                public int getOrdinal() {
                    return 0;
                }
            };

            return Collections.singletonList(c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
