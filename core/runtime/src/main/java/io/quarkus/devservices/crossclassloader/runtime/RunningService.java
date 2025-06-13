package io.quarkus.devservices.crossclassloader.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class RunningService implements Closeable, Supplier<Map<String, String>> {

    private final String feature;
    private final String description;
    private final Map<String, String> configs;
    private final String containerId;
    private final Consumer<RunningService> selfCloseable;

    public RunningService(String feature, String description, Map<String, String> configs, String containerId,
            Consumer<RunningService> selfCloseable) {
        this.feature = feature;
        this.description = description;
        this.configs = configs;
        this.containerId = containerId;
        this.selfCloseable = selfCloseable;
    }

    @Override
    public void close() throws IOException {
        selfCloseable.accept(this);
    }

    @Override
    public Map<String, String> get() {
        return configs;
    }

    public String feature() {
        return feature;
    }

    public String description() {
        return description;
    }

    public Map<String, String> configs() {
        return configs;
    }

    public String containerId() {
        return containerId;
    }

}
