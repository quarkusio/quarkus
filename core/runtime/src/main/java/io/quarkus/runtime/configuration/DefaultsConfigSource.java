package io.quarkus.runtime.configuration;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.KeyMap;
import io.smallrye.config.common.MapBackedConfigSource;

@StaticInitSafe
public class DefaultsConfigSource extends MapBackedConfigSource {
    private final KeyMap<String> wildcards;

    public DefaultsConfigSource(final Map<String, String> properties, final String name, final int ordinal) {
        // Defaults may contain wildcards, but we don't want to expose them in getPropertyNames, so we need to filter
        // them
        super(name, filterWildcards(properties), ordinal);
        this.wildcards = new KeyMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey().contains("*")) {
                this.wildcards.findOrAdd(entry.getKey()).putRootValue(entry.getValue());
            }
        }
    }

    @Override
    public String getValue(final String propertyName) {
        String value = super.getValue(propertyName);
        return value == null ? wildcards.findRootValue(propertyName) : value;
    }

    private static Map<String, String> filterWildcards(final Map<String, String> properties) {
        Map<String, String> filtered = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey().contains("*")) {
                continue;
            }
            filtered.put(entry.getKey(), entry.getValue());
        }
        return filtered;
    }
}
