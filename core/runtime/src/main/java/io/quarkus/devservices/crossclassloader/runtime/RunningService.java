package io.quarkus.devservices.crossclassloader.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

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
    private final Map<String, String> overrideConfigs;
    private final String containerId;
    private final Closeable closeable;

    public RunningService(String feature, String description, Map<String, String> configs, Map<String, String> overrideConfig,
            String containerId,
            Closeable closeable) {
        this.feature = feature;
        this.description = description;
        this.configs = configs;
        this.overrideConfigs = overrideConfig;
        this.containerId = containerId;
        this.closeable = closeable;
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

    /**
     * @deprecated Subject to changes due to <a href="https://github.com/quarkusio/quarkus/pull/51209">#51209</a>
     */
    @Deprecated(forRemoval = true)
    public Map<String, String> overrideConfigs() {
        return overrideConfigs;
    }

    public String containerId() {
        return containerId;
    }
}
