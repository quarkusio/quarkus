package io.quarkus.runtime.configuration;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class DisableableConfigSource implements ConfigSource {
    private final ConfigSource source;
    private final ConfigSource emptySource;

    private AtomicReference<ConfigSource> activeSource;

    public DisableableConfigSource(final ConfigSource source) {
        this.source = source;
        this.emptySource = new ConfigSource() {
            @Override
            public Set<String> getPropertyNames() {
                return Collections.emptySet();
            }

            @Override
            public String getValue(final String propertyName) {
                return null;
            }

            @Override
            public String getName() {
                return source.getName();
            }

            @Override
            public int getOrdinal() {
                return source.getOrdinal();
            }
        };
        activeSource = new AtomicReference<>(source);
    }

    @Override
    public Map<String, String> getProperties() {
        return activeSource.get().getProperties();
    }

    @Override
    public Set<String> getPropertyNames() {
        return activeSource.get().getPropertyNames();
    }

    @Override
    public int getOrdinal() {
        return source.getOrdinal();
    }

    @Override
    public String getValue(final String propertyName) {
        return activeSource.get().getValue(propertyName);
    }

    @Override
    public String getName() {
        return source.getName();
    }

    public void enable() {
        activeSource.compareAndSet(emptySource, source);
    }

    public void disable() {
        activeSource.compareAndSet(source, emptySource);
    }
}
