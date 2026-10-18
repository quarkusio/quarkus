package io.quarkus.observability.devresource;

import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class DevResourcesConfigSource implements ConfigSource {
    @Override
    public Set<String> getPropertyNames() {
        return DevResources.ensureStarted().keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return DevResources.ensureStarted().get(propertyName);
    }

    @Override
    public String getName() {
        return "DevResourcesConfigSource";
    }

    @Override
    public int getOrdinal() {
        // greater than any default Microprofile ConfigSource
        return 500;
    }
}
