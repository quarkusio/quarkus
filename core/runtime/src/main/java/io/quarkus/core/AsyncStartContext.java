package io.quarkus.core;

/**
 * A context which is consumed by a service that starts asynchronously
 * and optionally needs to register a handler for stopping.
 * <p>
 * Exactly one completion method ({@link #startComplete(Object)}, {@link #startFailed(Throwable)},
 * {@link #startCanceled()}) must be called to signal the outcome of the async start.
 * The completion methods follow a precedence model:
 * <ul>
 * <li>Failure is final: no other completion method may be called after
 * {@link #startFailed(Throwable)}, and {@code startFailed} may not be called
 * after any other completion method.</li>
 * <li>Cancellation supersedes completion: {@link #startComplete(Object)} may not be
 * called after {@link #startCanceled()}.</li>
 * <li>Cancellation after completion is harmless: calling {@link #startCanceled()} after
 * {@link #startComplete(Object)} is a no-op.</li>
 * <li>Cancellation is idempotent: calling {@link #startCanceled()} more than once
 * has no additional effect.</li>
 * <li>For typed services, {@link #startComplete(Object)} is <em>not</em> idempotent:
 * calling it more than once throws {@link IllegalStateException}.</li>
 * </ul>
 *
 * @param <T> the service type
 */
public interface AsyncStartContext<T> extends StartContext {
    /**
     * Indicate that the service start is complete.
     * For typed services, this method is not idempotent.
     *
     * @param value the service value (must not be {@code null})
     * @throws IllegalStateException if any completion method has already been called
     */
    void startComplete(T value);

    /**
     * Indicate that the service start was canceled.
     * Dependents which consume this service non-optionally will fail to start.
     * Dependents which consume this service optionally will receive an empty {@code Optional}.
     * <p>
     * This method is idempotent, and is a harmless no-op if {@link #startComplete(Object)}
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
