package io.quarkus.core;

/**
 * A context which is consumed by a void service that starts asynchronously
 * and optionally needs to register a handler for stopping.
 * <p>
 * Unlike {@link AsyncStartContext}, this context is for services that
 * produce no value ({@link Void}-typed services). The {@link #startComplete()}
 * method takes no arguments.
 * <p>
 * Exactly one completion method ({@link #startComplete()}, {@link #startFailed(Throwable)},
 * {@link #startCanceled()}) must be called to signal the outcome of the async start.
 * The completion methods follow a precedence model:
 * <ul>
 * <li>Failure is final: no other completion method may be called after
 * {@link #startFailed(Throwable)}, and {@code startFailed} may not be called
 * after any other completion method.</li>
 * <li>Cancellation supersedes completion: {@link #startComplete()} may not be
 * called after {@link #startCanceled()}.</li>
 * <li>Cancellation after completion is harmless: calling {@link #startCanceled()} after
 * {@link #startComplete()} is a no-op.</li>
 * <li>Both {@link #startComplete()} and {@link #startCanceled()} are individually
 * idempotent.</li>
 * </ul>
 */
public interface AsyncVoidStartContext extends StartContext {
    /**
     * Indicate that the service start is complete.
     * This method is idempotent; subsequent calls have no effect.
     *
     * @throws IllegalStateException if {@link #startCanceled()} or
     *         {@link #startFailed(Throwable)} has already been called
     */
    void startComplete();

    /**
     * Indicate that the service start was canceled.
     * Dependents which ordered themselves after this service will not be affected;
     * however, dependents which consume this service non-optionally will fail to start.
     * <p>
     * This method is idempotent, and is a harmless no-op if {@link #startComplete()}
     * was already called.
     *
     * @throws IllegalStateException if {@link #startFailed(Throwable)} has already been called
     */
    void startCanceled();

    /**
     * Indicate that the service start has failed.
     * This method must be the first and only completion method called.
     *
     * @param e the thrown exception (must not be {@code null})
     * @throws IllegalStateException if any completion method has already been called
     */
    void startFailed(Throwable e);
}
