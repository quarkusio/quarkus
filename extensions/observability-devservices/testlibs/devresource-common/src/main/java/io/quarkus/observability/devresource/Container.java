package io.quarkus.observability.devresource;

import java.io.Closeable;
import java.time.Duration;

import io.quarkus.observability.common.config.ContainerConfig;
import io.quarkus.runtime.LaunchMode;

/**
 * Simple container abstraction, e.g. similar to GenericContainer
 */
public interface Container<T extends ContainerConfig> {
    void start();

    void stop();

    String getContainerId();

    void withStartupTimeout(Duration duration);

    void configureDevServicesLabels(LaunchMode launchMode);

    Closeable closeableCallback(String serviceName);
}
