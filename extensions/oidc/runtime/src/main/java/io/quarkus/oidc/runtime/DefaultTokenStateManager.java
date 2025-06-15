package io.quarkus.oidc.runtime;

import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager.Strategy;
import io.quarkus.security.AuthenticationFailedException;
import io.smallrye.jwt.algorithm.KeyEncryptionAlgorithm;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class DefaultTokenStateManager implements TokenStateManager {
    private static final Logger LOG = Logger.getLogger(DefaultTokenStateManager.class);

    @Override
    public Uni<String> createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            AuthorizationCodeTokens tokens, OidcRequestContext<String> requestContext) {

        if (!oidcConfig.tokenStateManager().splitTokens()) {
            // ID, access and refresh tokens are all represented by a single cookie.
            // In this case they are all encrypted once all tokens have been added to the buffer.

            StringBuilder sb = new StringBuilder();

            // Add ID token
            sb.append(tokens.getIdToken());

            // By default, all three tokens are retained
            if (oidcConfig.tokenStateManager().strategy() == Strategy.KEEP_ALL_TOKENS) {
                // Add access and refresh tokens
                sb.append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append(tokens.getAccessToken())
                        .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append(tokens.getAccessTokenExpiresIn() != null ? tokens.getAccessTokenExpiresIn() : "")
                        .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append(tokens.getAccessTokenScope() != null ? encodeScopes(oidcConfig, tokens.getAccessTokenScope())
                                : "")
                        .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append(tokens.getRefreshToken());
            } else if (oidcConfig.tokenStateManager().strategy() == Strategy.ID_REFRESH_TOKENS) {
                // But sometimes the access token is not required.
                // For example, when the Quarkus endpoint does not need to use it to access another service.
                // Skip access token, access token expiry, access token scope, add refresh token
                sb.append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append("")
                        .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append("")
                        .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append("")
                        .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append(tokens.getRefreshToken());
            }

            // Now all three tokens are encrypted
            String encryptedTokens = encryptToken(sb.toString(), routingContext, oidcConfig);
            return Uni.createFrom().item(encryptedTokens);
        } else {
            // ID, access and refresh tokens are represented as individual cookies

            // Encrypt ID token
            String encryptedIdToken = encryptToken(tokens.getIdToken(), routingContext, oidcConfig);

            // By default, all three tokens are retained
            if (oidcConfig.tokenStateManager().strategy() == Strategy.KEEP_ALL_TOKENS) {

                StringBuilder sb = new StringBuilder();

                // Add access token and its expires_in property
                sb.append(tokens.getAccessToken())
                        .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append(tokens.getAccessTokenExpiresIn() != null ? tokens.getAccessTokenExpiresIn() : "")
                        .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                        .append(tokens.getAccessTokenScope() != null ? encodeScopes(oidcConfig, tokens.getAccessTokenScope())
                                : "");

                // Encrypt access token and create a `q_session_at` cookie.
                CodeAuthenticationMechanism.createCookie(routingContext,
                        oidcConfig,
                        getAccessTokenCookieName(oidcConfig),
                        encryptToken(sb.toString(), routingContext, oidcConfig),
                        routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM), true);

                // Encrypt refresh token and create a `q_session_rt` cookie.
                if (tokens.getRefreshToken() != null) {
                    CodeAuthenticationMechanism.createCookie(routingContext,
                            oidcConfig,
                            getRefreshTokenCookieName(oidcConfig),
                            encryptToken(tokens.getRefreshToken(), routingContext, oidcConfig),
                            routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM), true);
                }
            } else if (oidcConfig.tokenStateManager().strategy() == Strategy.ID_REFRESH_TOKENS
                    && tokens.getRefreshToken() != null) {
                // Encrypt refresh token and create a `q_session_rt` cookie.
                CodeAuthenticationMechanism.createCookie(routingContext,
                        oidcConfig,
                        getRefreshTokenCookieName(oidcConfig),
                        encryptToken(tokens.getRefreshToken(), routingContext, oidcConfig),
                        routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM));
            }

            // q_session cookie
            return Uni.createFrom().item(encryptedIdToken);
        }

    }

    @Override
    public Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<AuthorizationCodeTokens> requestContext) {

        String idToken = null;
        String accessToken = null;
        Long accessTokenExpiresIn = null;
        String accessTokenScope = null;
        String refreshToken = null;

        if (!oidcConfig.tokenStateManager().splitTokens()) {
            // ID, access and refresh tokens are all be represented by a single cookie.

            String decryptedTokenState = decryptToken(tokenState, routingContext, oidcConfig);

            String[] tokens = CodeAuthenticationMechanism.COOKIE_PATTERN.split(decryptedTokenState);

            try {
                idToken = tokens[0];

                if (oidcConfig.tokenStateManager().strategy() == Strategy.KEEP_ALL_TOKENS) {
                    accessToken = tokens[1];
                    accessTokenExpiresIn = tokens[2].isEmpty() ? null : parseAccessTokenExpiresIn(tokens[2]);
                    accessTokenScope = tokens[3].isEmpty() ? null : tokens[3];
                    refreshToken = tokens[4];
                } else if (oidcConfig.tokenStateManager().strategy() == Strategy.ID_REFRESH_TOKENS) {
                    refreshToken = tokens[4];
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                final String error = "Session cookie is malformed";
                LOG.debug(ex);
                return Uni.createFrom().failure(new AuthenticationFailedException(error));
            } catch (AuthenticationFailedException ex) {
                return Uni.createFrom().failure(ex);
            }
        } else {
            // Decrypt ID token from the q_session cookie
            idToken = decryptToken(tokenState, routingContext, oidcConfig);

            if (oidcConfig.tokenStateManager().strategy() == Strategy.KEEP_ALL_TOKENS) {
                Cookie atCookie = getAccessTokenCookie(routingContext, oidcConfig);
                if (atCookie != null) {
                    // Decrypt access token from the q_session_at cookie
                    String accessTokenState = decryptToken(atCookie.getValue(), routingContext, oidcConfig);
                    String[] accessTokenData = CodeAuthenticationMechanism.COOKIE_PATTERN.split(accessTokenState);
                    accessToken = accessTokenData[0];
                    try {
                        accessTokenExpiresIn = accessTokenData[1].isEmpty() ? null
                                : parseAccessTokenExpiresIn(accessTokenData[1]);
                        if (accessTokenData.length == 3) {
                            accessTokenScope = accessTokenData[2].isEmpty() ? null
                                    : decodeScopes(oidcConfig, accessTokenData[2]);
                        }
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        final String error = "Session cookie is malformed";
                        LOG.debug(ex);
                        // Make this error message visible in the dev mode
                        return Uni.createFrom().failure(new AuthenticationFailedException(error));
                    } catch (AuthenticationFailedException ex) {
                        return Uni.createFrom().failure(ex);
                    }
                }
                Cookie rtCookie = getRefreshTokenCookie(routingContext, oidcConfig);
                if (rtCookie != null) {
                    // Decrypt refresh token from the q_session_rt cookie
                    refreshToken = decryptToken(rtCookie.getValue(), routingContext, oidcConfig);
                }
            } else if (oidcConfig.tokenStateManager().strategy() == Strategy.ID_REFRESH_TOKENS) {
                Cookie rtCookie = getRefreshTokenCookie(routingContext, oidcConfig);
                if (rtCookie != null) {
                    refreshToken = decryptToken(rtCookie.getValue(), routingContext, oidcConfig);
                }
            }
        }
        return Uni.createFrom()
                .item(new AuthorizationCodeTokens(idToken, accessToken, refreshToken, accessTokenExpiresIn, accessTokenScope));
    }

    @Override
    public Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<Void> requestContext) {
        if (oidcConfig.tokenStateManager().splitTokens()) {
            OidcUtils.removeCookie(routingContext, getAccessTokenCookie(routingContext, oidcConfig),
                    oidcConfig);
            OidcUtils.removeCookie(routingContext, getRefreshTokenCookie(routingContext, oidcConfig),
                    oidcConfig);
        }
        return CodeAuthenticationMechanism.VOID_UNI;
    }

    private static Long parseAccessTokenExpiresIn(String accessTokenExpiresInString) {
        try {
            return Long.valueOf(accessTokenExpiresInString);
        } catch (NumberFormatException ex) {
            final String error = """
                    Access token expires_in property in the session cookie must be a number, found %s
                    """.formatted(accessTokenExpiresInString);
            LOG.debug(ex);
            // Make this error message visible in the dev mode
            throw new AuthenticationFailedException(error);
        }
    }

    private static ServerCookie getAccessTokenCookie(RoutingContext routingContext, OidcTenantConfig oidcConfig) {
        return (ServerCookie) routingContext.request().getCookie(getAccessTokenCookieName(oidcConfig));
    }

    private static ServerCookie getRefreshTokenCookie(RoutingContext routingContext, OidcTenantConfig oidcConfig) {
        return (ServerCookie) routingContext.request().getCookie(getRefreshTokenCookieName(oidcConfig));
    }

    private static String getAccessTokenCookieName(OidcTenantConfig oidcConfig) {
        String cookieSuffix = OidcUtils.getCookieSuffix(oidcConfig);
        return OidcUtils.SESSION_AT_COOKIE_NAME + cookieSuffix;
    }

    private static String getRefreshTokenCookieName(OidcTenantConfig oidcConfig) {
        String cookieSuffix = OidcUtils.getCookieSuffix(oidcConfig);
        return OidcUtils.SESSION_RT_COOKIE_NAME + cookieSuffix;
    }

    private static String encryptToken(String token, RoutingContext context, OidcTenantConfig oidcConfig) {
        if (oidcConfig.tokenStateManager().encryptionRequired()) {
            TenantConfigContext configContext = context.get(TenantConfigContext.class.getName());
            try {
                KeyEncryptionAlgorithm encAlgorithm = KeyEncryptionAlgorithm
                        .valueOf(oidcConfig.tokenStateManager().encryptionAlgorithm().name());
                return OidcUtils.encryptString(token, configContext.getSessionCookieEncryptionKey(), encAlgorithm);
            } catch (Exception ex) {
                throw new AuthenticationFailedException(ex);
            }
        }
        return token;
    }

    private static String decryptToken(String token, RoutingContext context, OidcTenantConfig oidcConfig) {
        if (oidcConfig.tokenStateManager().encryptionRequired()) {
            TenantConfigContext configContext = context.get(TenantConfigContext.class.getName());
            try {
                KeyEncryptionAlgorithm encAlgorithm = KeyEncryptionAlgorithm
                        .valueOf(oidcConfig.tokenStateManager().encryptionAlgorithm().name());
                return OidcUtils.decryptString(token, configContext.getSessionCookieEncryptionKey(), encAlgorithm);
            } catch (Exception ex) {
                throw new AuthenticationFailedException(ex);
            }
        }
        return token;
    }

    private static String encodeScopes(OidcTenantConfig oidcConfig, String accessTokenScope) {
        if (oidcConfig.tokenStateManager().encryptionRequired()) {
            return accessTokenScope;
        }
        return OidcCommonUtils.base64UrlEncode(accessTokenScope.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeScopes(OidcTenantConfig oidcConfig, String accessTokenScope) {
        if (oidcConfig.tokenStateManager().encryptionRequired()) {
            return accessTokenScope;
        }
        return OidcCommonUtils.base64UrlDecode(accessTokenScope);
    }
}
