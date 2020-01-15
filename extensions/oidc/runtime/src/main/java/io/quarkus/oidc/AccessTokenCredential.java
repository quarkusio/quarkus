package io.quarkus.oidc;

import io.quarkus.oidc.runtime.ContextAwareTokenCredential;
import io.vertx.ext.web.RoutingContext;

public class AccessTokenCredential extends ContextAwareTokenCredential {

    private RefreshToken refreshToken;

    public AccessTokenCredential() {
        this(null, null);
    }

    /**
     * Create AccessTokenCredential
     * 
     * @param accessToken - access token
     */
    public AccessTokenCredential(String accessToken, RoutingContext context) {
        super(accessToken, "bearer", context);
    }

    /**
     * Create AccessTokenCredential
     * 
     * @param accessToken - access token
     * @param refreshToken - refresh token which can be used to refresh this access token, may be null
     */
    public AccessTokenCredential(String accessToken, RefreshToken refreshToken, RoutingContext context) {
        this(accessToken, context);
        this.refreshToken = refreshToken;
    }

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }
}
