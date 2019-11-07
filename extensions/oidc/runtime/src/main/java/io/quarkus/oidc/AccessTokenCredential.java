package io.quarkus.oidc;

import io.quarkus.security.credential.TokenCredential;

public class AccessTokenCredential extends TokenCredential {

    private String refreshToken;

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
    public AccessTokenCredential(String accessToken, String refreshToken) {
        super(accessToken, "bearer");
        if (accessToken.equals(refreshToken)) {
            throw new OIDCException("Access and refresh tokens can not be equal");
        }
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
