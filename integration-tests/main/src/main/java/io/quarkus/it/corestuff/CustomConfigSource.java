package io.quarkus.it.corestuff;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public final class CustomConfigSource implements ConfigSource {

    private static final Map<String, String> THE_MAP = Collections.singletonMap("test.custom.config", "custom");

    public Map<String, String> getProperties() {
        return THE_MAP;
    }

    public Set<String> getPropertyNames() {
        return THE_MAP.keySet();
    }

    public String getValue(final String propertyName) {
        return THE_MAP.get(propertyName);
    }

    public String getName() {
        return "Custom config source";
    }
}
