package io.quarkus.restclient.configuration;

import static java.util.Collections.emptySet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.common.MapBackedConfigSource;

/**
 * This simulates a build time only source to test the recording of configuration values. It is still discovered at
 * runtime, but it doesn't return any configuration.
 */
public class RestClientBuildTimeConfigSource extends MapBackedConfigSource {
    // Because getPropertyNames() is called during SmallRyeConfig init (3 times)
    private int propertyNamesCallCount = 0;

    private static final Map<String, String> PROPERTIES = Map.of(
            "io.quarkus.restclient.configuration.EchoClient/mp-rest/url", "http://nohost:${quarkus.http.test-port:8081}");

    public RestClientBuildTimeConfigSource() {
        super(RestClientBuildTimeConfigSource.class.getName(), new HashMap<>());
    }

    @Override
    public String getValue(final String propertyName) {
        if (!propertyName.equals("io.quarkus.restclient.configuration.EchoClient/mp-rest/url")) {
            return null;
        }

        if (isBuildTime()) {
            return "http://nohost";
        }

        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        if (propertyNamesCallCount >= 3) {
            return isBuildTime() ? PROPERTIES.keySet() : emptySet();
        } else {
            propertyNamesCallCount++;
            return emptySet();
        }
    }

    @Override
    public int getOrdinal() {
        return Integer.MAX_VALUE;
    }

    private static boolean isBuildTime() {
        // We can only call this when the SmallRyeConfig is already initialized, or else we may get into a loop
        Config config = ConfigProvider.getConfig();
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().equals("PropertiesConfigSource[source=Build system]")) {
                return true;
            }
        }
        return false;
    }
}
