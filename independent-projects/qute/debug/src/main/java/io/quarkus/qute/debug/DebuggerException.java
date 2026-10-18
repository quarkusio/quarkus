package io.quarkus.qute.debug;

/**
 * Base exception class for all Qute debugger-related errors.
 * <p>
 * This unchecked exception is thrown when an unexpected error occurs during a debugging session,
 * such as communication issues with the Debug Adapter Protocol (DAP), invalid debug events,
 * or internal failures in the Qute debugger implementation.
 * </p>
 * <p>
 * It serves as a common parent for more specific debugger exceptions and provides
 * constructors to wrap lower-level exceptions.
 * </p>
 */
public class DebuggerException extends RuntimeException {

    /**
     * Creates a new {@link DebuggerException} with the specified cause.
     *
     * @param e the underlying cause of this exception, may be {@code null}
     */
    public DebuggerException(Throwable e) {
        super(e);
    }

    /**
     * Creates a new {@link DebuggerException} without any message or cause.
     * <p>
     * This constructor is typically used when the context of the error
     * is sufficient to identify the problem without additional details.
     * </p>
     */
    public DebuggerException() {
    }
}
