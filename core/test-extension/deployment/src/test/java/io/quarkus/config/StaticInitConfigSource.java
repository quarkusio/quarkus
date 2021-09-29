package io.quarkus.config;

import java.util.Map;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.common.MapBackedConfigSource;

@StaticInitSafe
public class StaticInitConfigSource extends MapBackedConfigSource {
    public StaticInitConfigSource() {
        super("", Map.of("config.static.init.my-prop", "1234"));
    }
}
