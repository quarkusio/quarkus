package io.quarkus.devservice.runtime.config;

import static io.quarkus.runtime.configuration.ConfigSourceOrdinal.DEV_SERVICES_OVERRIDE;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @deprecated Subject to changes due to <a href="https://github.com/quarkusio/quarkus/pull/51209">#51209</a>
 */
@Deprecated(forRemoval = true)
public class DevServicesOverrideConfigSource implements ConfigSource {

    private static volatile Map<String, String> config = Map.of();

    public static void setConfig(Map<String, String> config) {
        DevServicesOverrideConfigSource.config = config;
    }

    @Override
    public Set<String> getPropertyNames() {
        return config.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return config.get(propertyName);
    }

    @Override
    public String getName() {
        return "DevServicesOverrideConfigSource";
    }

    @Override
    public int getOrdinal() {
        return DEV_SERVICES_OVERRIDE.getOrdinal();
    }
}
