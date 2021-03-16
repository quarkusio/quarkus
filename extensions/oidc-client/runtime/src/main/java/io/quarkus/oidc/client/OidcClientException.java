package io.quarkus.oidc.client;

@SuppressWarnings("serial")
public class OidcClientException extends RuntimeException {
    public OidcClientException() {

    }

    public OidcClientException(String errorMessage) {
        this(errorMessage, null);
    }

    public OidcClientException(Throwable cause) {
        this(null, cause);
    }

    public OidcClientException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
