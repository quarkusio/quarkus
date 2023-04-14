package io.quarkus.observability.devresource.testcontainers;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.observability.common.config.ContainerConfig;
import io.quarkus.observability.devresource.Container;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;

/**
 * A container resource abstraction
 */
public abstract class ContainerResource<T extends GenericContainer<T>, C extends ContainerConfig>
        implements DevResourceLifecycleManager<C> {

    protected T container;
    protected Container<C> wrapper;

    protected Container<C> set(T container) {
        this.container = container;
        this.wrapper = new TestcontainerContainer<>(container);
        return this.wrapper;
    }

    @Override
    public Map<String, String> start() {
        if (container == null) {
            set(defaultContainer());
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
