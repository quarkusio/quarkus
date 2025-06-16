package io.quarkus.deployment.builditem;

import java.io.Closeable;

public interface Startable extends Closeable {
    void start();

    String getConnectionInfo();

    // This starts to couple to containers, so we could move it to sub-interface and use that in dev services
    String getContainerId();

}
