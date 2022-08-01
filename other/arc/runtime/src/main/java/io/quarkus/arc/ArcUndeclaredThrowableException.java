package io.quarkus.arc;

/**
 * Exception that is thrown from generated arc classes if a checked exception cannot be propagated
 */
public class ArcUndeclaredThrowableException extends RuntimeException {

    public ArcUndeclaredThrowableException(Throwable cause) {
        super(cause);
    }

    public ArcUndeclaredThrowableException() {
        super();
    }

    public ArcUndeclaredThrowableException(String message) {
        super(message);
    }

    public ArcUndeclaredThrowableException(String message, Throwable cause) {
        super(message, cause);
    }

    protected ArcUndeclaredThrowableException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
