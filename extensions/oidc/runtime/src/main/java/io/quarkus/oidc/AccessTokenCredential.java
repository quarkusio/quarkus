package io.quarkus.oidc;

import io.quarkus.security.credential.TokenCredential;

public class AccessTokenCredential extends TokenCredential {

    private RefreshToken refreshToken;

    public AccessTokenCredential() {
        this(null);
    }

    /**
     * Create AccessTokenCredential
     * 
     * @param accessToken - access token
     */
    public AccessTokenCredential(String accessToken) {
        this(accessToken, null);
    }

    /**
     * Create AccessTokenCredential
     * 
     * @param accessToken - access token
     * @param refreshToken - refresh token which can be used to refresh this access token, may be null
     */
    public AccessTokenCredential(String accessToken, RefreshToken refreshToken) {
        super(accessToken, "bearer");
        this.refreshToken = refreshToken;
    }

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }
}
