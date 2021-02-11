package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.shutdown.ShutdownListener;

public final class ShutdownListenerBuildItem extends MultiBuildItem {

    final ShutdownListener shutdownListener;

    public ShutdownListenerBuildItem(ShutdownListener shutdownListener) {
        this.shutdownListener = shutdownListener;
    }

    public ShutdownListener getShutdownListener() {
        return shutdownListener;
    }
}
