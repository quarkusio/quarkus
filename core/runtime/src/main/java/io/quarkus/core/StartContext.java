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

    /**
     * Register a synchronous graceful pre-shutdown handler.
     * During the pre-shutdown phase the application should continue to function
     * normally, but notify external systems (such as readiness probes) that it
     * is about to shut down.
     * The handler is considered complete when it returns.
     * If an exception is thrown, it will be logged and the handler will be
     * considered complete.
     *
     * @param handler the pre-shutdown handler (must not be {@code null})
     */
    default void onGracefulPreShutdown(Runnable handler) {
        onGracefulPreShutdownAsync(ctxt -> {
            try {
                handler.run();
            } finally {
                ctxt.done();
            }
        });
    }

    /**
     * Register an asynchronous graceful pre-shutdown handler.
     * During the pre-shutdown phase the application should continue to function
     * normally, but notify external systems (such as readiness probes) that it
     * is about to shut down.
     * The context passed to the handler must be used to signal completion via
     * {@link AsyncGracefulShutdownContext#done()}.
     * The handler must not throw exceptions, because any thrown exceptions
     * cannot be captured.
     *
     * @param handler the pre-shutdown handler (must not be {@code null})
     */
    void onGracefulPreShutdownAsync(Consumer<AsyncGracefulShutdownContext> handler);

    /**
     * Register a synchronous graceful shutdown handler.
     * During the shutdown phase the application should reject new external
     * requests but allow existing in-flight requests to complete.
     * The handler is considered complete when it returns.
     * If an exception is thrown, it will be logged and the handler will be
     * considered complete.
     *
     * @param handler the shutdown handler (must not be {@code null})
     */
    default void onGracefulShutdown(Runnable handler) {
        onGracefulShutdownAsync(ctxt -> {
            try {
                handler.run();
            } finally {
                ctxt.done();
            }
        });
    }

    /**
     * Register an asynchronous graceful shutdown handler.
     * During the shutdown phase the application should reject new external
     * requests but allow existing in-flight requests to complete.
     * The context passed to the handler must be used to signal completion via
     * {@link AsyncGracefulShutdownContext#done()}.
     * The handler must not throw exceptions, because any thrown exceptions
     * cannot be captured.
     *
     * @param handler the shutdown handler (must not be {@code null})
     */
    void onGracefulShutdownAsync(Consumer<AsyncGracefulShutdownContext> handler);
}
