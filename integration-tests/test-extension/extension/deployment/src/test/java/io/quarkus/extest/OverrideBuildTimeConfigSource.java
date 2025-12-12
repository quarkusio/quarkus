package io.quarkus.extest;

import java.util.Map;
import java.util.Set;

import io.smallrye.config.common.MapBackedConfigSource;

public class OverrideBuildTimeConfigSource extends MapBackedConfigSource {
    public OverrideBuildTimeConfigSource() {
        super("OverrideBuildTimeConfigSource", Map.of("quarkus.mapping.btrt.unlisted", "value"));
    }

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }
}
