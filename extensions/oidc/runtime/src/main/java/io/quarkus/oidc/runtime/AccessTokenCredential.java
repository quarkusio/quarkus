package io.quarkus.oidc.runtime;

import io.quarkus.security.credential.TokenCredential;

public class AccessTokenCredential extends TokenCredential {

    private String refreshToken;

    public AccessTokenCredential(String accessToken) {
        this(accessToken, null);
    }

    public AccessTokenCredential(String accessToken, String refreshToken) {
        super(accessToken, "bearer");
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
