package io.quarkus.runtime.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.runtime.util.ClassPathUtils;
import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;

public class QuarkusPropertiesConfigSourceProvider implements ConfigSourceProvider {

    private List<ConfigSource> configSources = new ArrayList<>();

    public QuarkusPropertiesConfigSourceProvider(String propertyFileName, boolean optional, ClassLoader classLoader) {
        try {
            Enumeration<URL> propertyFileUrls = classLoader.getResources(propertyFileName);

            if (!optional && !propertyFileUrls.hasMoreElements()) {
                throw new IllegalStateException(propertyFileName + " wasn't found.");
            }

            while (propertyFileUrls.hasMoreElements()) {
                URL propertyFileUrl = propertyFileUrls.nextElement();
                configSources.add(new PropertiesConfigSource(propertyFileUrl));
            }
        } catch (IOException ioe) {
            throw new IllegalStateException("problem while loading microprofile-config.properties files", ioe);
        }

    }

    @Override
    public List<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return configSources;
    }

    private static class PropertiesConfigSource extends MapBackedConfigSource {
        private static final long serialVersionUID = 1866835565147832432L;

        private static final String NAME_PREFIX = "PropertiesConfigSource[source=";

        /**
         * Construct a new instance
         *
         * @param url a property file location
         * @throws IOException if an error occurred when reading from the input stream
         */
        public PropertiesConfigSource(URL url) throws IOException {
            super(NAME_PREFIX + url.toString() + "]", urlToMap(url));
        }

        public PropertiesConfigSource(Properties properties, String source) {
            super(NAME_PREFIX + source + "]", ConfigSourceUtil.propertiesToMap(properties));
        }

        public PropertiesConfigSource(Map<String, String> properties, String source, int ordinal) {
            super(NAME_PREFIX + source + "]", properties, ordinal);
        }
    }

    private static Map<String, String> urlToMap(URL url) throws IOException {
        final Properties props = new Properties();
        ClassPathUtils.consumeStream(url, is -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                props.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return ConfigSourceUtil.propertiesToMap(props);
    }
}
