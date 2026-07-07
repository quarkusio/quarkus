package io.quarkus.devservices.common;

import java.util.function.Function;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import io.quarkus.deployment.builditem.Startable;

/**
 * A wrapper for a {@link GenericContainer} that implements the {@link Startable} interface.
 *
 * @param <T> the type of the container
 */
public class StartableContainer<T extends GenericContainer<?>> implements Startable {

    private static final Logger LOG = Logger.getLogger(StartableContainer.class);

    private final T container;
    private final Function<T, String> connectionInfoFunction;

    public StartableContainer(T container) {
        this(container, null);
    }

    public StartableContainer(T container, Function<T, String> connectionInfoFunction) {
        this.container = container;
        this.connectionInfoFunction = connectionInfoFunction;
    }

    @Override
    public void start() {
        container.start();
    }

    @Override
    public String getConnectionInfo() {
        if (connectionInfoFunction == null) {
            return null;
        }
        return connectionInfoFunction.apply(container);
    }

    public T getContainer() {
        return container;
    }

    @Override
    public String getContainerId() {
        return container.getContainerId();
    }

    @Override
    public void close() {
        if (TestcontainersConfiguration.getInstance().environmentSupportsReuse()
                && container.isShouldBeReused()) {
            LOG.infof("Not stopping Dev Services container %s as reuse is enabled", container.getContainerId());
            return;
        }
        container.close();
    }
}
