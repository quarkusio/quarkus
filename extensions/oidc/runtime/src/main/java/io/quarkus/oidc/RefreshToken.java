package io.quarkus.oidc;

import io.quarkus.security.credential.TokenCredential;

/**
 * Represents a refresh token issued to the application.
 */
public class RefreshToken extends TokenCredential {

    public RefreshToken() {
        this(null);
    }

    public RefreshToken(String token) {
        super(token, "refresh_token");
    }

    // TODO: more methods to help the application to refresh tokens
}
