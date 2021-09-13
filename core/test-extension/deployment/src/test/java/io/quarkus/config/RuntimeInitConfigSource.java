package io.quarkus.config;

import java.util.Map;

import io.smallrye.config.common.MapBackedConfigSource;

public class RuntimeInitConfigSource extends MapBackedConfigSource {
    public RuntimeInitConfigSource() {
        super("", Map.of("config.static.init.my-prop", "1234"));
    }
}
