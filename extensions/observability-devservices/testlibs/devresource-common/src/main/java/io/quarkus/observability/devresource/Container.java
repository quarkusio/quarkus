package io.quarkus.observability.devresource;

import java.io.Closeable;
import java.time.Duration;

import io.quarkus.observability.common.config.ContainerConfig;

/**
 * Simple container abstraction, e.g. similar to GenericContainer
 */
public interface Container<T extends ContainerConfig> {
    void start();

    void stop();

    String getContainerId();

    void withStartupTimeout(Duration duration);

    Closeable closeableCallback(String serviceName);
}
