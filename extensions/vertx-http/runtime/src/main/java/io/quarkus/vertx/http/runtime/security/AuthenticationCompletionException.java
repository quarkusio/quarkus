package io.quarkus.vertx.http.runtime.security;

/**
 * Exception indicating that a user authentication flow has not been completed and no further challenge is required.
 * For example, it is used to avoid the redirect loops during an OIDC authorization code flow after the user has
 * already authenticated at the IDP site but the redirect back to Quarkus has the invalid state parameter or cookie
 * or if the code flow can not be completed.
 */
public class AuthenticationCompletionException extends RuntimeException {

    public AuthenticationCompletionException() {

    }

    public AuthenticationCompletionException(String errorMessage) {
        this(errorMessage, null);
    }

    public AuthenticationCompletionException(Throwable cause) {
        this(null, cause);
    }

    public AuthenticationCompletionException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
