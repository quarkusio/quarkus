package io.quarkus.oidc.client.runtime;

@SuppressWarnings("serial")
public class DisabledOidcClientException extends RuntimeException {
    public DisabledOidcClientException() {

    }

    public DisabledOidcClientException(String errorMessage) {
        this(errorMessage, null);
    }

    public DisabledOidcClientException(Throwable cause) {
        this(null, cause);
    }

    public DisabledOidcClientException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
