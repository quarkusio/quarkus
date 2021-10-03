package io.quarkus.oidc.runtime;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CacheTokenStateManager implements TokenStateManager {

    private static final String SESSION_AT_COOKIE_NAME = CodeAuthenticationMechanism.SESSION_COOKIE_NAME + "_at";
    private static final String SESSION_RT_COOKIE_NAME = CodeAuthenticationMechanism.SESSION_COOKIE_NAME + "_rt";

    @Override
    public Uni<String> createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig, AuthorizationCodeTokens tokens, OidcRequestContext<String> requestContext) {

        if (oidcConfig.tokenStateManager.strategy == OidcTenantConfig.TokenStateManager.Strategy.KEEP_ALL_TOKENS) {
            CodeAuthenticationMechanism.createCookie(routingContext,
                    oidcConfig,
                    getAccessTokenCookieName(oidcConfig.getTenantId().get()),
                    tokens.getAccessToken(),
                    routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM));
            if (tokens.getRefreshToken() != null) {
                CodeAuthenticationMechanism.createCookie(routingContext,
                        oidcConfig,
                        getRefreshTokenCookieName(oidcConfig.getTenantId().get()),
                        tokens.getRefreshToken(),
                        routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM));
            }
        } else if (oidcConfig.tokenStateManager.strategy == OidcTenantConfig.TokenStateManager.Strategy.ID_REFRESH_TOKENS) {
            if (tokens.getRefreshToken() != null) {
                CodeAuthenticationMechanism.createCookie(routingContext,
                        oidcConfig,
                        getRefreshTokenCookieName(oidcConfig.getTenantId().get()),
                        tokens.getRefreshToken(),
                        routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM));
            }
        }
        return Uni.createFrom().item(tokens.getIdToken());
    }

    @Override
    @CacheResult(cacheName = "tokenState")
    public Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, @CacheKey String tokenState, OidcRequestContext<AuthorizationCodeTokens> requestContext) {

        String idToken = tokenState;
        String accessToken = null;
        String refreshToken = null;
        if (oidcConfig.tokenStateManager.strategy == OidcTenantConfig.TokenStateManager.Strategy.KEEP_ALL_TOKENS) {
            Cookie atCookie = getAccessTokenCookie(routingContext, oidcConfig);
            if (atCookie != null) {
                accessToken = atCookie.getValue();
            }
            Cookie rtCookie = getRefreshTokenCookie(routingContext, oidcConfig);
            if (rtCookie != null) {
                refreshToken = rtCookie.getValue();
            }
        } else if (oidcConfig.tokenStateManager.strategy == OidcTenantConfig.TokenStateManager.Strategy.ID_REFRESH_TOKENS) {
            Cookie rtCookie = getRefreshTokenCookie(routingContext, oidcConfig);
            if (rtCookie != null) {
                refreshToken = rtCookie.getValue();
            }
        }

        return Uni.createFrom().item(new AuthorizationCodeTokens(idToken, accessToken, refreshToken));
    }

    @Override
    @CacheInvalidate(cacheName = "tokenState")
    public Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, @CacheKey String tokenState, OidcRequestContext<Void> requestContext) {
        CodeAuthenticationMechanism.removeCookie(routingContext, getAccessTokenCookie(routingContext, oidcConfig),
                oidcConfig);
        CodeAuthenticationMechanism.removeCookie(routingContext, getRefreshTokenCookie(routingContext, oidcConfig),
                oidcConfig);
        return CodeAuthenticationMechanism.VOID_UNI;
    }

    private static ServerCookie getAccessTokenCookie(RoutingContext routingContext, OidcTenantConfig oidcConfig) {
        return (ServerCookie) routingContext.request().getCookie(getAccessTokenCookieName(oidcConfig.getTenantId().get()));
    }

    private static ServerCookie getRefreshTokenCookie(RoutingContext routingContext, OidcTenantConfig oidcConfig) {
        return (ServerCookie) routingContext.request().getCookie(getRefreshTokenCookieName(oidcConfig.getTenantId().get()));
    }

    private static String getAccessTokenCookieName(String tenantId) {
        String cookieSuffix = CodeAuthenticationMechanism.getCookieSuffix(tenantId);
        return SESSION_AT_COOKIE_NAME + cookieSuffix;
    }

    private static String getRefreshTokenCookieName(String tenantId) {
        String cookieSuffix = CodeAuthenticationMechanism.getCookieSuffix(tenantId);
        return SESSION_RT_COOKIE_NAME + cookieSuffix;
    }
}
