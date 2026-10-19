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

    /**
     * Apply Quarkus process ownership labels ({@code QUARKUS_PROCESS_UUID},
     * {@code QUARKUS_LAUNCH_MODE}) so {@code ContainerLocator} does not rediscover
     * this JVM's containers as shared during hot reload (issue #57067).
     * No-op by default; implementations wrapping a Testcontainers
     * {@code GenericContainer} should override and delegate to {@code ConfigureUtil}.
     *
     * @param launchModeName the {@link io.quarkus.runtime.LaunchMode#name()} of
     *        the current launch mode; implementations that need the full enum
     *        should resolve it via {@link io.quarkus.runtime.LaunchMode#valueOf(String)}.
     */
    default void configureDevServicesLabels(String launchModeName) {
    }

    Closeable closeableCallback(String serviceName);
}
