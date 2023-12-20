package io.quarkus.oidc.runtime;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class DefaultTokenStateManager implements TokenStateManager {

    private static final String SESSION_AT_COOKIE_NAME = OidcUtils.SESSION_COOKIE_NAME + "_at";
    private static final String SESSION_RT_COOKIE_NAME = OidcUtils.SESSION_COOKIE_NAME + "_rt";

    @Override
    public Uni<String> createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            AuthorizationCodeTokens tokens, OidcRequestContext<String> requestContext) {

        boolean encryptAll = !oidcConfig.tokenStateManager.splitTokens;

        StringBuilder sb = new StringBuilder();
        sb.append(encryptAll ? tokens.getIdToken() : encryptToken(tokens.getIdToken(), routingContext, oidcConfig));
        if (oidcConfig.tokenStateManager.strategy == OidcTenantConfig.TokenStateManager.Strategy.KEEP_ALL_TOKENS) {
            if (!oidcConfig.tokenStateManager.splitTokens) {
                sb.append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append(encryptAll ? tokens.getAccessToken()
                                : encryptToken(tokens.getAccessToken(), routingContext, oidcConfig))
                        .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append(encryptAll ? tokens.getRefreshToken()
                                : encryptToken(tokens.getRefreshToken(), routingContext, oidcConfig));
            } else {
                CodeAuthenticationMechanism.createCookie(routingContext,
                        oidcConfig,
                        getAccessTokenCookieName(oidcConfig),
                        encryptToken(tokens.getAccessToken(), routingContext, oidcConfig),
                        routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM), true);
                if (tokens.getRefreshToken() != null) {
                    CodeAuthenticationMechanism.createCookie(routingContext,
                            oidcConfig,
                            getRefreshTokenCookieName(oidcConfig),
                            encryptToken(tokens.getRefreshToken(), routingContext, oidcConfig),
                            routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM), true);
                }
            }
        } else if (oidcConfig.tokenStateManager.strategy == OidcTenantConfig.TokenStateManager.Strategy.ID_REFRESH_TOKENS) {
            if (!oidcConfig.tokenStateManager.splitTokens) {
                sb.append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append("")
                        .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append(encryptAll ? tokens.getRefreshToken()
                                : encryptToken(tokens.getRefreshToken(), routingContext, oidcConfig));
            } else {
                if (tokens.getRefreshToken() != null) {
                    CodeAuthenticationMechanism.createCookie(routingContext,
                            oidcConfig,
                            getRefreshTokenCookieName(oidcConfig),
                            encryptToken(tokens.getRefreshToken(), routingContext, oidcConfig),
                            routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM));
                }
            }
        }
        String state = encryptAll ? encryptToken(sb.toString(), routingContext, oidcConfig) : sb.toString();
        return Uni.createFrom().item(state);
    }

    @Override
    public Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<AuthorizationCodeTokens> requestContext) {
        boolean decryptAll = !oidcConfig.tokenStateManager.splitTokens;

        tokenState = decryptAll ? decryptToken(tokenState, routingContext, oidcConfig) : tokenState;

        String[] tokens = CodeAuthenticationMechanism.COOKIE_PATTERN.split(tokenState);

        String idToken = decryptAll ? tokens[0] : decryptToken(tokens[0], routingContext, oidcConfig);

        String accessToken = null;
        String refreshToken = null;
        try {
            if (oidcConfig.tokenStateManager.strategy == OidcTenantConfig.TokenStateManager.Strategy.KEEP_ALL_TOKENS) {
                if (!oidcConfig.tokenStateManager.splitTokens) {
                    accessToken = decryptAll ? tokens[1] : decryptToken(tokens[1], routingContext, oidcConfig);
                    refreshToken = decryptAll ? tokens[2] : decryptToken(tokens[2], routingContext, oidcConfig);
                } else {
                    Cookie atCookie = getAccessTokenCookie(routingContext, oidcConfig);
                    if (atCookie != null) {
                        accessToken = decryptToken(atCookie.getValue(), routingContext, oidcConfig);
                    }
                    Cookie rtCookie = getRefreshTokenCookie(routingContext, oidcConfig);
                    if (rtCookie != null) {
                        refreshToken = decryptToken(rtCookie.getValue(), routingContext, oidcConfig);
                    }
                }
            } else if (oidcConfig.tokenStateManager.strategy == OidcTenantConfig.TokenStateManager.Strategy.ID_REFRESH_TOKENS) {
                if (!oidcConfig.tokenStateManager.splitTokens) {
                    refreshToken = decryptAll ? tokens[2] : decryptToken(tokens[2], routingContext, oidcConfig);
                } else {
                    Cookie rtCookie = getRefreshTokenCookie(routingContext, oidcConfig);
                    if (rtCookie != null) {
                        refreshToken = decryptToken(rtCookie.getValue(), routingContext, oidcConfig);
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            return Uni.createFrom().failure(new AuthenticationCompletionException("Session cookie is malformed"));
        }

        return Uni.createFrom().item(new AuthorizationCodeTokens(idToken, accessToken, refreshToken));
    }

    @Override
    public Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<Void> requestContext) {
        if (oidcConfig.tokenStateManager.splitTokens) {
            OidcUtils.removeCookie(routingContext, getAccessTokenCookie(routingContext, oidcConfig),
                    oidcConfig);
            OidcUtils.removeCookie(routingContext, getRefreshTokenCookie(routingContext, oidcConfig),
                    oidcConfig);
        }
        return CodeAuthenticationMechanism.VOID_UNI;
    }

    private static ServerCookie getAccessTokenCookie(RoutingContext routingContext, OidcTenantConfig oidcConfig) {
        return (ServerCookie) routingContext.request().getCookie(getAccessTokenCookieName(oidcConfig));
    }

    private static ServerCookie getRefreshTokenCookie(RoutingContext routingContext, OidcTenantConfig oidcConfig) {
        return (ServerCookie) routingContext.request().getCookie(getRefreshTokenCookieName(oidcConfig));
    }

    private static String getAccessTokenCookieName(OidcTenantConfig oidcConfig) {
        String cookieSuffix = OidcUtils.getCookieSuffix(oidcConfig);
        return SESSION_AT_COOKIE_NAME + cookieSuffix;
    }

    private static String getRefreshTokenCookieName(OidcTenantConfig oidcConfig) {
        String cookieSuffix = OidcUtils.getCookieSuffix(oidcConfig);
        return SESSION_RT_COOKIE_NAME + cookieSuffix;
    }

    private String encryptToken(String token, RoutingContext context, OidcTenantConfig oidcConfig) {
        if (oidcConfig.tokenStateManager.encryptionRequired) {
            TenantConfigContext configContext = context.get(TenantConfigContext.class.getName());
            try {
                return OidcUtils.encryptString(token, configContext.getTokenEncSecretKey());
            } catch (Exception ex) {
                throw new AuthenticationFailedException(ex);
            }
        }
        return token;
    }

    private String decryptToken(String token, RoutingContext context, OidcTenantConfig oidcConfig) {
        if (oidcConfig.tokenStateManager.encryptionRequired) {
            TenantConfigContext configContext = context.get(TenantConfigContext.class.getName());
            try {
                return OidcUtils.decryptString(token, configContext.getTokenEncSecretKey());
            } catch (Exception ex) {
                throw new AuthenticationFailedException(ex);
            }
        }
        return token;
    }
}
