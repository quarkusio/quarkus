package io.quarkus.websockets.next.runtime.devmode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Holds the dev mode hot replacement callback invoked when a server endpoint receives a message.
 * <p>
 * The scan runs on a dedicated thread: message handlers run on the Vert.x event loop and a restart needs
 * to stop the application, which must not happen on that thread.
 */
public class WebSocketHotReplacementInterceptor {

    private static volatile Supplier<Boolean> onMessageAction;
    private static volatile ExecutorService scanExecutor;
    private static final AtomicBoolean scanPending = new AtomicBoolean();

    static void register(Supplier<Boolean> onMessage) {
        scanExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "websockets-next-dev-scan");
            t.setDaemon(true);
            return t;
        });
        onMessageAction = onMessage;
    }

    static void shutdown() {
        ExecutorService executor = scanExecutor;
        scanExecutor = null;
        onMessageAction = null;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Trigger a scan for source changes, if dev mode hot replacement is active and no scan is pending yet.
     */
    public static void messageReceived() {
        Supplier<Boolean> action = onMessageAction;
        ExecutorService executor = scanExecutor;
        if (action == null || executor == null || !scanPending.compareAndSet(false, true)) {
            return;
        }
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            executor.submit(() -> {
                try {
                    Thread.currentThread().setContextClassLoader(tccl);
                    return action.get();
                } finally {
                    scanPending.set(false);
                }
            });
        } catch (RejectedExecutionException ignored) {
            // shut down between the null check and the submission
            scanPending.set(false);
        }
    }
}
