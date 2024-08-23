package io.quarkus.arc.impl.bcextensions;

import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.inject.build.compatible.spi.Parameters;

public class ParametersImpl implements Parameters {
    private final Map<String, Object> data;

    public ParametersImpl(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(type);
        return type.cast(data.get(key));
    }

    @Override
    public <T> T get(String key, Class<T> type, T defaultValue) {
        T result = get(key, type);
        return result != null ? result : defaultValue;
    }
}
