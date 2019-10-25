package io.quarkus.oidc.runtime;

import io.quarkus.security.credential.TokenCredential;

public class RefreshToken extends TokenCredential {

    public RefreshToken() {
        this(null);
    }

    public RefreshToken(String token) {
        super(token, "refresh_token");
    }
}
