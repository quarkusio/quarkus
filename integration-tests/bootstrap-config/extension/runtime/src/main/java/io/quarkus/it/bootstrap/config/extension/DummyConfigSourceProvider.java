package io.quarkus.it.bootstrap.config.extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.common.MapBackedConfigSource;

public class DummyConfigSourceProvider implements ConfigSourceProvider {
    private final DummyConfig dummyConfig;

    public DummyConfigSourceProvider(DummyConfig dummyConfig) {
        this.dummyConfig = dummyConfig;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        // Just copy the bootstrap config values to a new source
        Map<String, String> properties = new HashMap<>();
        properties.put("quarkus.dummy.name", dummyConfig.name);
        properties.put("quarkus.dummy.times", dummyConfig.times.toString());
        dummyConfig.map.forEach((key, mapConfig) -> properties.put("quarkus.dummy.map." + key, mapConfig.value));
        return List.of(new MapBackedConfigSource("bootstrap", properties, Integer.MAX_VALUE) {
        });
    }
}
