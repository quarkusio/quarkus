package io.quarkus.core;

import java.util.function.Consumer;

/**
 * A context which may be consumed by a service that needs to register
 * a handler for stopping.
 */
public interface StartContext {
    /**
     * Register a synchronous stop handler.
     * The service is considered to be stopped when the task returns.
     * If an exception is thrown by the handler, it will be logged,
     * and the service will be considered to be stopped.
     *
     * @param stopper the stop handler (must not be {@code null})
     */
    default void onStop(Runnable stopper) {
        onStopAsync(ctxt -> {
            try {
                stopper.run();
            } finally {
                ctxt.stopComplete();
            }
        });
    }

    /**
     * Register an asynchronous stop handler.
     * The context passed to the handler must be used to indicate when the
     * service is stopped.
     * The handler is initially called directly from the calling thread,
     * but may then schedule subsequent asynchronous work to be done.
     * The handler (and any related subordinate task) must not throw exceptions,
     * because any thrown exceptions cannot be captured.
     *
     * @param stopper the stop handler (must not be {@code null})
     */
    void onStopAsync(Consumer<AsyncStopContext> stopper);
}
