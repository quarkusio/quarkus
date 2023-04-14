package io.quarkus.observability.devresource;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.observability.common.config.ContainerConfig;

/**
 * A container resource abstraction
 */
public abstract class ContainerResource<T extends GenericContainer<T>, C extends ContainerConfig>
        implements DevResourceLifecycleManager<C> {

    protected T container;

    protected T set(T container) {
        this.container = container;
        return container;
    }

    @Override
    public Map<String, String> start() {
        if (container == null) {
            container = defaultContainer();
        }
        container.start();
        return doStart();
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    protected abstract T defaultContainer();

    protected abstract Map<String, String> doStart();
}
