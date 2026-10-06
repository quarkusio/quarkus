package io.quarkus.core;

/**
 * A context which is passed to an asynchronous graceful shutdown handler
 * registered via {@link StartContext#onGracefulPreShutdownAsync} or
 * {@link StartContext#onGracefulShutdownAsync}.
 * The handler must call {@link #done()} when its work is complete so that
 * the shutdown sequence can proceed to the next phase.
 */
public interface AsyncGracefulShutdownContext {
    /**
     * Signal that this handler's graceful shutdown work is complete.
     * This method is idempotent (calling it again will have no additional effect).
     */
    void done();
}
