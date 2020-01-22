package io.quarkus.it.bootstrap.config.extension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.runtime.ApplicationConfig;

/**
 * A dummy provider that returns a single ConfigSource which contains
 * as many properties as DummyConfig.times indicates and where the
 * key depends on DummyConfig.name
 */
public class DummyConfigSourceProvider implements ConfigSourceProvider {

    private final DummyConfig dummyConfig;
    private final ApplicationConfig applicationConfig;

    public DummyConfigSourceProvider(DummyConfig dummyConfig, ApplicationConfig applicationConfig) {
        this.dummyConfig = dummyConfig;
        this.applicationConfig = applicationConfig;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        InMemoryConfigSource configSource = new InMemoryConfigSource(Integer.MIN_VALUE, "dummy config source");
        for (int i = 0; i < dummyConfig.times; i++) {
            configSource.add(dummyConfig.name + ".key.i" + (i + 1), applicationConfig.name.get() + (i + 1));
        }
        return Collections.singletonList(configSource);
    }

    private static final class InMemoryConfigSource implements ConfigSource {

        private final Map<String, String> values = new HashMap<>();
        private final int ordinal;
        private final String name;

        private InMemoryConfigSource(int ordinal, String name) {
            this.ordinal = ordinal;
            this.name = name;
        }

        public void add(String key, String value) {
            values.put(key, value);
        }

        @Override
        public Map<String, String> getProperties() {
            return values;
        }

        @Override
        public Set<String> getPropertyNames() {
            return values.keySet();
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public String getValue(String propertyName) {
            return values.get(propertyName);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
