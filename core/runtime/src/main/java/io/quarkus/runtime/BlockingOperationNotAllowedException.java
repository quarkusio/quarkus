package io.quarkus.runtime;

/**
 * Exception that is thrown when a blocking operation is performed on the IO thread.
 */
public class BlockingOperationNotAllowedException extends IllegalStateException {

    public BlockingOperationNotAllowedException() {
        super();
    }

    public BlockingOperationNotAllowedException(String s) {
        super(s);
    }

    public BlockingOperationNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlockingOperationNotAllowedException(Throwable cause) {
        super(cause);
    }
}
