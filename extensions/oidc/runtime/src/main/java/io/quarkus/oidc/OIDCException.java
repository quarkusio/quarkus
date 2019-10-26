package io.quarkus.oidc;

@SuppressWarnings("serial")
public class OIDCException extends RuntimeException {
    public OIDCException() {
    }

    public OIDCException(String message) {
        this(message, null);
    }

    public OIDCException(Throwable cause) {
        this(null, cause);
    }

    public OIDCException(String message, Throwable cause) {
        super(message, cause);
    }
}
