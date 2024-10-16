package io.quarkus.smallrye.openapi.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.openapi.api.SmallRyeOASConfig;

public class MediaTypeConfigSource implements ConfigSource {

    private final Map<String, String> mediaTypes = new HashMap<>();

    public MediaTypeConfigSource() {
        mediaTypes.put(SmallRyeOASConfig.DEFAULT_PRODUCES_STREAMING, "application/octet-stream");
        mediaTypes.put(SmallRyeOASConfig.DEFAULT_CONSUMES_STREAMING, "application/octet-stream");
        mediaTypes.put(SmallRyeOASConfig.DEFAULT_PRODUCES, "application/json");
        mediaTypes.put(SmallRyeOASConfig.DEFAULT_CONSUMES, "application/json");
        mediaTypes.put(SmallRyeOASConfig.DEFAULT_PRODUCES_PRIMITIVES, "text/plain");
        mediaTypes.put(SmallRyeOASConfig.DEFAULT_CONSUMES_PRIMITIVES, "text/plain");
    }

    @Override
    public Set<String> getPropertyNames() {
        return mediaTypes.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return mediaTypes.get(propertyName);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

}
