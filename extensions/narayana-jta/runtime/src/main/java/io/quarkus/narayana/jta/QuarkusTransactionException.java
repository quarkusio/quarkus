package io.quarkus.narayana.jta;

/**
 * Runtime exception that is used to wrap any checked exceptions thrown from the {@link QuarkusTransaction} methods.
 */
public class QuarkusTransactionException extends RuntimeException {

    public QuarkusTransactionException(Throwable cause) {
        super(cause);
    }

    public QuarkusTransactionException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public QuarkusTransactionException(String message) {
        super(message);
    }

    public QuarkusTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
