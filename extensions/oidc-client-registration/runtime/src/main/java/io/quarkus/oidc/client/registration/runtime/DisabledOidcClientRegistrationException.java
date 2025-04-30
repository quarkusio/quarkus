package io.quarkus.oidc.client.registration.runtime;

/**
 * Exception which indicates that an injected {@link OidcClientRegistration} is disabled
 * with the `quarkus.oidc-client-registration.registration-enabled=false` property.
 */
@SuppressWarnings("serial")
public class DisabledOidcClientRegistrationException extends RuntimeException {
    public DisabledOidcClientRegistrationException() {

    }

    public DisabledOidcClientRegistrationException(String errorMessage) {
        this(errorMessage, null);
    }

    public DisabledOidcClientRegistrationException(Throwable cause) {
        this(null, cause);
    }

    public DisabledOidcClientRegistrationException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
