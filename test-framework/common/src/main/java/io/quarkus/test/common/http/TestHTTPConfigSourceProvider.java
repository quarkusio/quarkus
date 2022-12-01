package io.quarkus.test.common.http;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 *
 */
public class TestHTTPConfigSourceProvider implements ConfigSourceProvider {

    static final String TEST_URL_VALUE = "http://${quarkus.http.host:localhost}:${quarkus.http.test-port:8081}${quarkus.http.root-path:${quarkus.servlet.context-path:}}";
    static final String TEST_URL_KEY = "test.url";

    static final String TEST_URL_SSL_VALUE = "https://${quarkus.http.host:localhost}:${quarkus.http.test-ssl-port:8444}${quarkus.http.root-path:${quarkus.servlet.context-path:}}";
    static final String TEST_URL_SSL_KEY = "test.url.ssl";

    static final Map<String, String> entries = Map.of(TEST_URL_KEY, sanitizeURL(TEST_URL_VALUE),
            TEST_URL_SSL_KEY, sanitizeURL(TEST_URL_SSL_VALUE),
            "%dev." + TEST_URL_KEY, sanitizeURL(
                    "http://${quarkus.http.host:localhost}:${quarkus.http.test-port:8080}${quarkus.http.root-path:${quarkus.servlet.context-path:}}"));

    private static String sanitizeURL(String url) {
        return url.replace("0.0.0.0", "localhost");
    }

    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        return Collections.singletonList(new TestURLConfigSource());
    }

    static class TestURLConfigSource implements ConfigSource, Serializable {
        private static final long serialVersionUID = 4841094273900625000L;

        public Map<String, String> getProperties() {
            return entries;
        }

        public Set<String> getPropertyNames() {
            return entries.keySet();
        }

        public String getValue(final String propertyName) {
            return entries.get(propertyName);
        }

        public String getName() {
            return "test URL provider";
        }

        public int getOrdinal() {
            return Integer.MIN_VALUE + 1000;
        }
    }
}
