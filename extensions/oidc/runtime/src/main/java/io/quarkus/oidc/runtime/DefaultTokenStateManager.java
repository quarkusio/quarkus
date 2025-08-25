package io.quarkus.oidc.runtime;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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

                createSessionAccessTokenCookie(routingContext, oidcConfig, tokens);

                // Encrypt refresh token and create a `q_session_rt` cookie.
                if (tokens.getRefreshToken() != null) {
                    OidcUtils.createSessionCookie(routingContext,
                            oidcConfig,
                            getRefreshTokenCookieName(oidcConfig),
                            encryptToken(tokens.getRefreshToken(), routingContext, oidcConfig),
                            routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM));
                }
            } else if (oidcConfig.tokenStateManager().strategy() == Strategy.ID_REFRESH_TOKENS
                    && tokens.getRefreshToken() != null) {
                // Encrypt refresh token and create a `q_session_rt` cookie.
                OidcUtils.createSessionCookie(routingContext,
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
                String atCookieValue = getAccessTokenCookie(routingContext, oidcConfig);
                if (atCookieValue != null) {
                    // Decrypt access token from the q_session_at cookie
                    String accessTokenState = decryptToken(atCookieValue, routingContext, oidcConfig);
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
            getAccessTokenCookie(routingContext, oidcConfig);
            List<String> atCookieNames = routingContext.get(OidcUtils.SESSION_AT_COOKIE_NAME);
            if (atCookieNames != null) {
                LOG.debugf("Remove session access cookie names: %s", atCookieNames);
                for (String cookieName : atCookieNames) {
                    OidcUtils.removeCookie(routingContext, oidcConfig, cookieName);
                }
            }

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

    private static String getAccessTokenCookie(RoutingContext routingContext, OidcTenantConfig oidcConfig) {
        final Map<String, Cookie> cookies = routingContext.request().cookieMap();
        return OidcUtils.getSessionCookie(routingContext.data(), cookies, oidcConfig, OidcUtils.SESSION_AT_COOKIE_NAME,
                getAccessTokenCookieName(oidcConfig));
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

    private static void createSessionAccessTokenCookie(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            AuthorizationCodeTokens tokens) {

        String cookieName = getAccessTokenCookieName(oidcConfig);

        StringBuilder sb = new StringBuilder();

        // Add access token and its expires_in property
        sb.append(tokens.getAccessToken())
                .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                .append(tokens.getAccessTokenExpiresIn() != null ? tokens.getAccessTokenExpiresIn() : "")
                .append(CodeAuthenticationMechanism.COOKIE_DELIM)
                .append(tokens.getAccessTokenScope() != null ? encodeScopes(oidcConfig, tokens.getAccessTokenScope())
                        : "");

        String cookieValue = encryptToken(sb.toString(), routingContext, oidcConfig);

        LOG.debugf("Session access token cookie length for the tenant %s is %d bytes.",
                oidcConfig.tenantId().get(), cookieValue.length());
        if (cookieValue.length() > OidcUtils.MAX_COOKIE_VALUE_LENGTH) {
            LOG.debugf(
                    "Session access token cookie length for the tenant %s is greater than %d bytes."
                            + " The cookie will be split to chunks to avoid browsers ignoring it."
                            + " Alternative recommendations: 1. Set 'quarkus.oidc.token-state-manager.strategy=id-refresh-tokens' if you do not need to use the access token"
                            + " as a source of roles or to request UserInfo or propagate it to the downstream services."
                            + " 2. Decrease the encrypted session access token cookie's length by enabling a direct encryption algorithm"
                            + " with 'quarkus.oidc.token-state-manager.encryption-algorithm=dir'."
                            + " 3. Decrease the session access token cookie's length by disabling its encryption with 'quarkus.oidc.token-state-manager.encryption-required=false'"
                            + " but only if it is considered to be safe in your application's network."
                            + " 4. Use the 'quarkus-oidc-db-token-state-manager' extension or the 'quarkus-oidc-redis-token-state-manager' extension"
                            + " or register a custom 'quarkus.oidc.TokenStateManager'"
                            + " CDI bean with the alternative priority set to 1 and save the tokens on the server.",
                    oidcConfig.tenantId().get(), OidcUtils.MAX_COOKIE_VALUE_LENGTH);
            OidcUtils.createChunkedCookie(routingContext, oidcConfig, cookieName, cookieValue,
                    routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM));
        } else {
            // Create a `q_session_at` cookie.
            OidcUtils.createSessionCookie(routingContext,
                    oidcConfig,
                    cookieName,
                    cookieValue,
                    routingContext.get(CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM));
        }
    }
}
