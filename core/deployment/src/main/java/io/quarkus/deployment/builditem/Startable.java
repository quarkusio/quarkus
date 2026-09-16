package io.quarkus.deployment.builditem;

import java.io.Closeable;

public interface Startable extends Closeable {
    void start();

    String getConnectionInfo();

    // This starts to couple to containers, so we could move it to sub-interface and use that in dev services
    String getContainerId();

    /**
     * Whether this service is configured for Testcontainers-level reuse.
     * <p>
     * A reusable service survives the {@link #close()} Quarkus performs on shutdown, so that a later,
     * separate JVM run can reuse it. It is still closed when its configuration is no longer compatible
     * with the application being started, such as on an incompatible profile change.
     *
     * @return {@code true} if this service is configured for Testcontainers-level reuse
     */
    default boolean isReusable() {
        return false;
    }

}
