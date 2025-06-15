package io.quarkus.oidc;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.credential.TokenCredential;

public class AccessTokenCredential extends TokenCredential {

    private RefreshToken refreshToken;
    private boolean opaque;

    public AccessTokenCredential() {
        this(null);
    }

    /**
     * Create AccessTokenCredential
     *
     * @param accessToken
     *        - access token
     */
    public AccessTokenCredential(String accessToken) {
        this(accessToken, null);
    }

    /**
     * Create AccessTokenCredential
     *
     * @param accessToken
     *        - access token
     * @param refreshToken
     *        - refresh token which can be used to refresh this access token, may be null
     */
    public AccessTokenCredential(String accessToken, RefreshToken refreshToken) {
        super(accessToken, "bearer");
        this.refreshToken = refreshToken;
        if (accessToken != null) {
            this.opaque = OidcUtils.isOpaqueToken(accessToken);
        }
    }

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public boolean isOpaque() {
        return opaque;
    }
}
