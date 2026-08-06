package io.quarkus.deployment.builditem;

import java.io.Closeable;

public interface Startable extends Closeable {
    void start();

    String getConnectionInfo();

    // This starts to couple to containers, so we could move it to sub-interface and use that in dev services
    String getContainerId();

    /**
     * Whether this container is reusable across JVM restarts (Testcontainers reuse).
     * When {@code true}, the dev services registry will not close this container on
     * shutdown, allowing it to be reused by the next run.
     */
    default boolean isReusable() {
        return false;
    }
}
