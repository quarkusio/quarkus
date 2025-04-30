package io.quarkus.kubernetes.spi;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

public class Property<T> {

    private final String name;
    private final Class<T> type;
    private final Optional<T> value;
    private final T defaultValue;
    private final boolean runtime;

    public Property(String name, Class<T> type, Optional<T> value, T defaultValue, boolean runtime) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.defaultValue = defaultValue;
        this.runtime = runtime;
    }

    public static <T> Property<T> fromRuntimeConfiguration(String name, Class<T> type, T defaultValue) {
        return new Property<T>(name, type, ConfigProvider.getConfig().getOptionalValue(name, type), defaultValue, true);
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    public Optional<T> getValue() {
        return value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public boolean isRuntime() {
        return runtime;
    }
}
