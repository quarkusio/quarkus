package io.quarkus.extest.runtime.config.rename;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.common.MapBackedConfigSource;

public class RenameConfigSource extends MapBackedConfigSource {
    private static final Map<String, String> FALLBACK_PROPERTIES = Map.of(
            "quarkus.rename.prop", "1234",
            "quarkus.rename.only-in-new", "only-in-new",
            "quarkus.rename-old.only-in-old", "only-in-old",
            "quarkus.rename.in-both", "new",
            "quarkus.rename-old.in-both", "old",
            "quarkus.rename-old.with-default", "old-default");

    public RenameConfigSource() {
        super(RenameConfigSource.class.getName(), new HashMap<>());
    }

    @Override
    public String getValue(final String propertyName) {
        return FALLBACK_PROPERTIES.get(propertyName);
    }

    @Override
    public Set<String> getPropertyNames() {
        return FALLBACK_PROPERTIES.keySet();
    }
}
