package io.quarkus.oidc;

import io.quarkus.security.credential.TokenCredential;

public class RefreshToken extends TokenCredential {

    public RefreshToken() {
        this(null);
    }

    public RefreshToken(String refreshToken) {
        super(refreshToken, "refresh_token");
    }
}
