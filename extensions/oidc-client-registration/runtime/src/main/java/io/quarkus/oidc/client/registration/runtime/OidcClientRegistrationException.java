package io.quarkus.oidc.client.registration.runtime;

/**
 * Exception which indicates that an error has occurred during the {@link OidcClientRegistration}
 * initialization, default client registration or subsequent operations with the client registration endpoint.
 */
@SuppressWarnings("serial")
public class OidcClientRegistrationException extends RuntimeException {
    public OidcClientRegistrationException() {

    }

    public OidcClientRegistrationException(String errorMessage) {
        this(errorMessage, null);
    }

    public OidcClientRegistrationException(Throwable cause) {
        this(null, cause);
    }

    public OidcClientRegistrationException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
