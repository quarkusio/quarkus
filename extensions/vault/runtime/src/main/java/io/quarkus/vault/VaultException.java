package io.quarkus.vault;

public class VaultException extends RuntimeException {

    public VaultException() {
    }

    public VaultException(String message) {
        super(message);
    }

    public VaultException(String message, Throwable cause) {
        super(message, cause);
    }

    public VaultException(Throwable cause) {
        super(cause);
    }

    public VaultException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
