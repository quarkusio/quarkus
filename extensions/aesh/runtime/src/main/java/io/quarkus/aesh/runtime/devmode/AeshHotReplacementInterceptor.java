package io.quarkus.aesh.runtime.devmode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Static holder for the dev mode hot replacement callback.
 * <p>
 * Called by {@link io.quarkus.aesh.runtime.AeshRemoteConnectionHandler}
 * before setting up each new remote connection (SSH, WebSocket).
 * If source files have changed, this triggers an application restart.
 * <p>
 * The scan runs on a dedicated thread to avoid deadlocking the Netty
 * event loop. SSH connections are handled on Netty threads, and the
 * application restart needs to shut down the Netty event loop group —
 * calling {@code doScan()} directly on that thread would deadlock.
 */
public class AeshHotReplacementInterceptor {

    private static volatile Supplier<Boolean> onConnectionAction;

    private static final ExecutorService scanExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "aesh-dev-scan");
        t.setDaemon(true);
        return t;
    });

    static void register(Supplier<Boolean> onConnection) {
        onConnectionAction = onConnection;
    }

    /**
     * Returns {@code true} if dev mode hot replacement is active.
     */
    public static boolean isActive() {
        return onConnectionAction != null;
    }

    /**
     * Trigger a dev mode scan asynchronously. The scan runs on a separate
     * thread so it does not block the Netty event loop. If changes are
     * detected, the application restarts and the SSH/WebSocket server
     * is stopped, which drops all active connections.
     */
    public static void fireAsync() {
        Supplier<Boolean> action = onConnectionAction;
        if (action != null) {
            scanExecutor.submit(action::get);
        }
    }
}
