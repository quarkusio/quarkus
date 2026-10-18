package io.quarkus.devservice.runtime.config;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class DevServicesConfigSource implements ConfigSource {

    private static volatile Map<String, String> config = Map.of();

    public static void setConfig(Map<String, String> config) {
        DevServicesConfigSource.config = config;
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
        return "DevServicesConfigSource";
    }

    @Override
    public int getOrdinal() {
        return 240; // a bit less than application properties, because this config should only take effect if nothing is set
    }
}
