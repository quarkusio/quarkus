package io.quarkus.observability.devresource.testcontainers;

import java.io.Closeable;
import java.time.Duration;
import java.util.Objects;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.observability.common.config.ContainerConfig;
import io.quarkus.observability.devresource.Container;

/**
 * Container impl / wrapper for Testcontainer's GenericContainer
 */
public class TestcontainerContainer<C extends GenericContainer<C>, T extends ContainerConfig> implements Container<T> {
    private final GenericContainer<C> container;

    public TestcontainerContainer(GenericContainer<C> container) {
        this.container = Objects.requireNonNull(container);
    }

    @Override
    public void start() {
        container.start();
    }

    @Override
    public void stop() {
        container.stop();
    }

    @Override
    public String getContainerId() {
        return container.getContainerId();
    }

    @Override
    public void withStartupTimeout(Duration duration) {
        container.withStartupTimeout(duration);
    }

    @Override
    public Closeable closeableCallback(String serviceName) {
        return new ContainerShutdownCloseable(container, serviceName);
    }
}
