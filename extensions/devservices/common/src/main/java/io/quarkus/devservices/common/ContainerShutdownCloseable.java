package io.quarkus.devservices.common;

import java.io.Closeable;
import java.util.Objects;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Helper to define the stop strategy for containers created by DevServices.
 * <p>
 * In particular, we don't want to actually stop the containers when they
 * have been flagged for reuse, and when the Testcontainers configuration
 * has been explicitly set to allow container reuse.
 * To enable reuse, add {@literal testcontainers.reuse.enable=true} in your
 * {@literal .testcontainers.properties} file, to be stored in your home.
 *
 * @see <a href="https://www.testcontainers.org/features/configuration/">Testcontainers Configuration</a>.
 */
public final class ContainerShutdownCloseable implements Closeable {

    private static final Logger LOG = Logger.getLogger(ContainerShutdownCloseable.class);

    private final GenericContainer<?> container;
    private final String friendlyServiceName;

    /**
     * @param container the container to be eventually closed
     * @param friendlyServiceName for logging purposes
     */
    public ContainerShutdownCloseable(GenericContainer<?> container, String friendlyServiceName) {
        Objects.requireNonNull(container);
        Objects.requireNonNull(friendlyServiceName);
        this.container = container;
        this.friendlyServiceName = friendlyServiceName;
    }

    @Override
    public void close() {
        if (TestcontainersConfiguration.getInstance().environmentSupportsReuse()
                && container.isShouldBeReused()) {
            LOG.infof(
                    "Dev Services for %s is no longer needed by this Quarkus instance, but is not shut down as 'testcontainers.reuse.enable' is enabled in your Testcontainers configuration file",
                    friendlyServiceName);
        } else {
            container.stop();
            LOG.infof("Dev Services for %s shut down.", this.friendlyServiceName);
        }
    }

}
