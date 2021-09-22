package io.quarkus.vault.runtime;

import io.quarkus.vault.VaultException;

public class VaultIOException extends VaultException {

    public VaultIOException() {
    }

    public VaultIOException(String message) {
        super(message);
    }

    public VaultIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public VaultIOException(Throwable cause) {
        super(cause);
    }

    public VaultIOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
