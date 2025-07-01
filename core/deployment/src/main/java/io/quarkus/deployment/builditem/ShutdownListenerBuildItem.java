package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.shutdown.ShutdownListener;

/**
 * A build item that holds a {@link ShutdownListener} instance.
 * <p>
 * Allows registration of listeners to be notified during application shutdown.
 */
public final class ShutdownListenerBuildItem extends MultiBuildItem {

    final ShutdownListener shutdownListener;

    public ShutdownListenerBuildItem(ShutdownListener shutdownListener) {
        this.shutdownListener = shutdownListener;
    }

    public ShutdownListener getShutdownListener() {
        return shutdownListener;
    }
}
