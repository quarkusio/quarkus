package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.shutdown.ShutdownListener;

/**
 * A build item that holds a {@link ShutdownListener} instance.
 * <p>
 * Allows registration of listeners to be notified during application shutdown.
 * <p>
 * It should be noted, that in most cases, this build item should not be used and instead extensions should opt for
 * {@link ShutdownContext} (via {@link ShutdownContextBuildItem})
 *
 * @deprecated Use {@link io.quarkus.core.StartContext#onGracefulPreShutdown(Runnable)} and
 *             {@link io.quarkus.core.StartContext#onGracefulShutdown(Runnable)} (or their async variants) instead.
 */
@Deprecated(since = "4.0", forRemoval = true)
public final class ShutdownListenerBuildItem extends MultiBuildItem {

    final ShutdownListener shutdownListener;

    public ShutdownListenerBuildItem(ShutdownListener shutdownListener) {
        this.shutdownListener = shutdownListener;
    }

    public ShutdownListener getShutdownListener() {
        return shutdownListener;
    }
}
