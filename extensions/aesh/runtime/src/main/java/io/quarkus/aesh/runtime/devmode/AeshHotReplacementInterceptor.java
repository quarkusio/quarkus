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

    // Lazily initialized to avoid creating an ExecutorService during
    // native image build (the Cleaner it carries is not allowed in the heap).
    private static volatile ExecutorService scanExecutor;

    public static void register(Supplier<Boolean> onConnection) {
        // Publish the executor before the action so that fireAsync()
        // never sees onConnectionAction != null with scanExecutor still null.
        scanExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "aesh-dev-scan");
            t.setDaemon(true);
            return t;
        });
        onConnectionAction = onConnection;
    }

    /**
     * Returns {@code true} if dev mode hot replacement is active.
     */
    public static boolean isActive() {
        return onConnectionAction != null;
    }

    /**
     * Shut down the executor and clear the callback. Called from
     * {@link AeshHotReplacementSetup#close()} on dev mode restart.
     * <p>
     * Fields are nulled in reverse order of {@link #register}: scanExecutor
     * before onConnectionAction, so that {@link #fireAsync()} never sees
     * a non-null action with a null executor.
     */
    public static void shutdown() {
        ExecutorService exec = scanExecutor;
        scanExecutor = null;
        onConnectionAction = null;
        if (exec != null) {
            exec.shutdownNow();
        }
    }

    /**
     * Trigger a dev mode scan asynchronously. The scan runs on a separate
     * thread so it does not block the Netty event loop. If changes are
     * detected, the application restarts and the SSH/WebSocket server
     * is stopped, which drops all active connections.
     */
    public static void fireAsync() {
        Supplier<Boolean> action = onConnectionAction;
        ExecutorService exec = scanExecutor;
        if (action != null && exec != null) {
            try {
                exec.submit(action::get);
            } catch (java.util.concurrent.RejectedExecutionException e) {
                // Executor was shut down between our null check and submit call
            }
        }
    }
}
