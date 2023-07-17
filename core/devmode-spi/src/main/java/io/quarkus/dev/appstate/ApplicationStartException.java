package io.quarkus.dev.appstate;

/**
 * Exception that is reported if the application fails to start
 *
 * This exception has already been logged when this exception is generated,
 * so should not be logged again
 */
public class ApplicationStartException extends RuntimeException {

    public ApplicationStartException(Throwable cause) {
        super(cause);
    }
}
