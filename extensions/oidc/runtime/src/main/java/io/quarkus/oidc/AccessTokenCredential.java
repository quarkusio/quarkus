package io.quarkus.oidc;

import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.ext.web.RoutingContext;

public class AccessTokenCredential extends OidcTokenCredential {

    private RefreshToken refreshToken;
    private boolean opaque;

    public AccessTokenCredential() {
        this(null, null);
    }

    /**
     * Create AccessTokenCredential
     * 
     * @param accessToken - access token
     */
    public AccessTokenCredential(String accessToken, RoutingContext context) {
        this(accessToken, null, context);
    }

    /**
     * Create AccessTokenCredential
     * 
     * @param accessToken - access token
     * @param refreshToken - refresh token which can be used to refresh this access token, may be null
     */
    public AccessTokenCredential(String accessToken, RefreshToken refreshToken, RoutingContext context) {
        super(accessToken, "bearer", context);
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
