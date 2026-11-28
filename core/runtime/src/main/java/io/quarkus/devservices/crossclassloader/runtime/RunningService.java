package io.quarkus.devservices.crossclassloader.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Represents a running service in Dev Services.
 * This class is used to encapsulate the details of a service that has been started by Dev Services,
 * necessary for tracking and managing the lifecycle of the service.
 * <p>
 * It is designed to be loaded with the parent classloader, allowing it to be shared across different classloaders.
 */
public final class RunningService implements Closeable {

    private final String feature;
    private final String description;
    private final Map<String, String> configs;
    private final String containerId;
    private final Closeable closeable;
    private final boolean reusable;
    // Only JDK types, since the listener is called from the runtime classloader of each application start
    private final BiConsumer<String, String> valueListener;

    public RunningService(String feature, String description, Map<String, String> configs,
            String containerId,
            Closeable closeable, boolean reusable) {
        this(feature, description, configs, containerId, closeable, reusable, (key, value) -> {
        });
    }

    public RunningService(String feature, String description, Map<String, String> configs,
            String containerId,
            Closeable closeable, boolean reusable,
            BiConsumer<String, String> valueListener) {
        this.feature = feature;
        this.description = description;
        this.configs = configs;
        this.containerId = containerId;
        this.closeable = closeable;
        this.reusable = reusable;
        this.valueListener = valueListener;
    }

    @Override
    public void close() throws IOException {
        closeable.close();
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

    public boolean isReusable() {
        return reusable;
    }

    /**
     * Hands this service a value of the running application.
     *
     * @param key the key of the value, as agreed between the service and the application
     * @param value the value
     */
    public void valueAvailable(String key, String value) {
        valueListener.accept(key, value);
    }
}
