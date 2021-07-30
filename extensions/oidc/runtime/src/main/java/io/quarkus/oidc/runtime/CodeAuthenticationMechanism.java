package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcIdentityProvider.NEW_AUTHENTICATION;
import static io.quarkus.oidc.runtime.OidcIdentityProvider.REFRESH_TOKEN_GRANT_RESPONSE;

import java.net.URI;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Authentication;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class CodeAuthenticationMechanism extends AbstractOidcAuthenticationMechanism {

    static final String AMP = "&";
    static final String EQ = "=";
    static final String COOKIE_DELIM = "|";
    static final Pattern COOKIE_PATTERN = Pattern.compile("\\" + COOKIE_DELIM);
    static final String SESSION_COOKIE_NAME = "q_session";
    static final String SESSION_MAX_AGE_PARAM = "session-max-age";

    private static final Logger LOG = Logger.getLogger(CodeAuthenticationMechanism.class);

    private static final String STATE_COOKIE_NAME = "q_auth";
    private static final String POST_LOGOUT_COOKIE_NAME = "q_post_logout";

    private static QuarkusSecurityIdentity augmentIdentity(SecurityIdentity securityIdentity,
            String accessToken,
            String refreshToken,
            RoutingContext context) {
        IdTokenCredential idTokenCredential = securityIdentity.getCredential(IdTokenCredential.class);
        RefreshToken refreshTokenCredential = new RefreshToken(refreshToken);
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(securityIdentity.getPrincipal())
                .addCredential(idTokenCredential)
                .addCredential(new AccessTokenCredential(accessToken, refreshTokenCredential, context))
                .addCredential(refreshTokenCredential)
                .addRoles(securityIdentity.getRoles())
                .addAttributes(securityIdentity.getAttributes())
                .addPermissionChecker(new Function<Permission, Uni<Boolean>>() {
                    @Override
                    public Uni<Boolean> apply(Permission permission) {
                        return securityIdentity.checkPermission(permission);
                    }
                }).build();
    }

    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {

        final Cookie sessionCookie = context.request().getCookie(getSessionCookieName(resolver.resolveConfig(context)));

        // if session already established, try to re-authenticate
        if (sessionCookie != null) {
            Uni<TenantConfigContext> resolvedContext = resolver.resolveContext(context);
            return resolvedContext.onItem()
                    .transformToUni(new Function<TenantConfigContext, Uni<? extends SecurityIdentity>>() {
                        @Override
                        public Uni<SecurityIdentity> apply(TenantConfigContext tenantContext) {
                            return reAuthenticate(sessionCookie, context, identityProviderManager, tenantContext);
                        }
                    });
        }

        final String code = context.request().getParam("code");
        if (code == null) {
            return Uni.createFrom().optional(Optional.empty());
        }

        // start a new session by starting the code flow dance
        Uni<TenantConfigContext> resolvedContext = resolver.resolveContext(context);
        return resolvedContext.onItem().transformToUni(new Function<TenantConfigContext, Uni<? extends SecurityIdentity>>() {
            @Override
            public Uni<SecurityIdentity> apply(TenantConfigContext tenantContext) {
                return performCodeFlow(identityProviderManager, context, tenantContext, code);
            }
        });
    }

    private Uni<SecurityIdentity> reAuthenticate(Cookie sessionCookie,
            RoutingContext context,
            IdentityProviderManager identityProviderManager,
            TenantConfigContext configContext) {

        AuthorizationCodeTokens session = resolver.getTokenStateManager().getTokens(context, configContext.oidcConfig,
                sessionCookie.getValue());

        context.put(OidcConstants.ACCESS_TOKEN_VALUE, session.getAccessToken());
        return authenticate(identityProviderManager, context, new IdTokenCredential(session.getIdToken(), context))
                .map(new Function<SecurityIdentity, SecurityIdentity>() {
                    @Override
                    public SecurityIdentity apply(SecurityIdentity identity) {
                        if (isLogout(context, configContext)) {
                            fireEvent(SecurityEvent.Type.OIDC_LOGOUT_RP_INITIATED, identity);
                            throw redirectToLogoutEndpoint(context, configContext, session.getIdToken());
                        }

                        return augmentIdentity(identity, session.getAccessToken(), session.getRefreshToken(), context);
                    }
                }).onFailure().recoverWithUni(new Function<Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<? extends SecurityIdentity> apply(Throwable t) {
                        if (t instanceof AuthenticationRedirectException) {
                            throw (AuthenticationRedirectException) t;
                        }

                        if (!(t instanceof TokenAutoRefreshException)) {
                            boolean expired = (t.getCause() instanceof InvalidJwtException)
                                    && ((InvalidJwtException) t.getCause()).hasErrorCode(ErrorCodes.EXPIRED);

                            if (!expired) {
                                LOG.debugf("Authentication failure: %s", t.getCause());
                                throw new AuthenticationCompletionException(t.getCause());
                            }
                            if (!configContext.oidcConfig.token.refreshExpired) {
                                LOG.debug("Token has expired, token refresh is not allowed");
                                throw new AuthenticationCompletionException(t.getCause());
                            }
                            LOG.debug("Token has expired, trying to refresh it");
                            return refreshSecurityIdentity(configContext, session.getRefreshToken(), context,
                                    identityProviderManager, false, null);
                        } else {
                            return refreshSecurityIdentity(configContext, session.getRefreshToken(), context,
                                    identityProviderManager, true,
                                    ((TokenAutoRefreshException) t).getSecurityIdentity());
                        }
                    }
                });

    }

    private boolean isJavaScript(RoutingContext context) {
        String value = context.request().getHeader("X-Requested-With");
        return "JavaScript".equals(value) || "XMLHttpRequest".equals(value);
    }

    // This test determines if the default behavior of returning a 302 should go forward
    // The only case that shouldn't return a 302 is if the call is a XHR and the 
    // user has set the auto direct application property to false indicating that
    // the client application will manually handle the redirect to account for SPA behavior
    private boolean shouldAutoRedirect(TenantConfigContext configContext, RoutingContext context) {
        return isJavaScript(context) ? configContext.oidcConfig.authentication.javaScriptAutoRedirect : true;
    }

    public Uni<ChallengeData> getChallenge(RoutingContext context) {

        Uni<TenantConfigContext> tenantContext = resolver.resolveContext(context);
        return tenantContext.onItem().transformToUni(new Function<TenantConfigContext, Uni<? extends ChallengeData>>() {
            @Override
            public Uni<ChallengeData> apply(TenantConfigContext tenantContext) {
                return getChallengeInternal(context, tenantContext);
            }
        });
    }

    public Uni<ChallengeData> getChallengeInternal(RoutingContext context, TenantConfigContext configContext) {
        removeCookie(context, configContext, getSessionCookieName(configContext.oidcConfig));

        if (!shouldAutoRedirect(configContext, context)) {
            // If the client (usually an SPA) wants to handle the redirect manually, then
            // return status code 499 and WWW-Authenticate header with the 'OIDC' value.
            return Uni.createFrom().item(new ChallengeData(499, "WWW-Authenticate", "OIDC"));
        }

        StringBuilder codeFlowParams = new StringBuilder();

        // response_type
        codeFlowParams.append(OidcConstants.CODE_FLOW_RESPONSE_TYPE).append(EQ).append(OidcConstants.CODE_FLOW_CODE);

        // client_id
        codeFlowParams.append(AMP).append(OidcConstants.CLIENT_ID).append(EQ)
                .append(OidcCommonUtils.urlEncode(configContext.oidcConfig.clientId.get()));

        // scope
        List<String> scopes = new ArrayList<>();
        scopes.add("openid");
        configContext.oidcConfig.getAuthentication().scopes.ifPresent(scopes::addAll);
        codeFlowParams.append(AMP).append(OidcConstants.TOKEN_SCOPE).append(EQ)
                .append(OidcCommonUtils.urlEncode(String.join(" ", scopes)));

        // redirect_uri
        String redirectPath = getRedirectPath(configContext, context);
        String redirectUriParam = buildUri(context, isForceHttps(configContext), redirectPath);
        LOG.debugf("Authentication request redirect_uri parameter: %s", redirectUriParam);

        codeFlowParams.append(AMP).append(OidcConstants.CODE_FLOW_REDIRECT_URI).append(EQ)
                .append(OidcCommonUtils.urlEncode(redirectUriParam));

        // state
        codeFlowParams.append(AMP).append(OidcConstants.CODE_FLOW_STATE).append(EQ)
                .append(generateCodeFlowState(context, configContext, redirectPath));

        // extra redirect parameters, see https://openid.net/specs/openid-connect-core-1_0.html#AuthRequests
        if (configContext.oidcConfig.authentication.getExtraParams() != null) {
            for (Map.Entry<String, String> entry : configContext.oidcConfig.authentication.getExtraParams().entrySet()) {
                codeFlowParams.append(AMP).append(entry.getKey()).append(EQ)
                        .append(OidcCommonUtils.urlEncode(entry.getValue()));
            }
        }

        String authorizationURL = configContext.provider.getMetadata().getAuthorizationUri() + "?" + codeFlowParams.toString();

        return Uni.createFrom().item(new ChallengeData(HttpResponseStatus.FOUND.code(), HttpHeaders.LOCATION,
                authorizationURL));
    }

    private Uni<SecurityIdentity> performCodeFlow(IdentityProviderManager identityProviderManager,
            RoutingContext context, TenantConfigContext configContext, String code) {

        Cookie stateCookie = context.getCookie(getStateCookieName(configContext));

        String userPath = null;
        String userQuery = null;
        if (stateCookie != null) {
            List<String> values = context.queryParam("state");
            // IDP must return a 'state' query parameter and the value of the state cookie must start with this parameter's value
            if (values.size() != 1) {
                LOG.debug("State parameter can not be empty or multi-valued");
                return Uni.createFrom().failure(new AuthenticationCompletionException());
            } else if (!stateCookie.getValue().startsWith(values.get(0))) {
                LOG.debug("State cookie value does not match the state query parameter value");
                return Uni.createFrom().failure(new AuthenticationCompletionException());
            } else {
                // This is an original redirect from IDP, check if the original request path and query need to be restored
                String[] pair = COOKIE_PATTERN.split(stateCookie.getValue());
                if (pair.length == 2) {
                    int userQueryIndex = pair[1].indexOf("?");
                    if (userQueryIndex >= 0) {
                        userPath = pair[1].substring(0, userQueryIndex);
                        if (userQueryIndex + 1 < pair[1].length()) {
                            userQuery = pair[1].substring(userQueryIndex + 1);
                        }
                    } else {
                        userPath = pair[1];
                    }
                }
                removeCookie(context, configContext, getStateCookieName(configContext));
            }
        } else {
            // State cookie must be available to minimize the risk of CSRF
            LOG.debug("The state cookie is missing after a redirect from IDP, authentication has failed");
            return Uni.createFrom().failure(new AuthenticationCompletionException());
        }

        final String finalUserPath = userPath;
        final String finalUserQuery = userQuery;

        Uni<AuthorizationCodeTokens> codeFlowTokensUni = getCodeFlowTokensUni(context, configContext, code);

        return codeFlowTokensUni
                .onItemOrFailure()
                .transformToUni(new BiFunction<AuthorizationCodeTokens, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(final AuthorizationCodeTokens tokens, final Throwable tOuter) {

                        if (tOuter != null) {
                            LOG.debugf("Exception during the code to token exchange: %s", tOuter.getMessage());
                            return Uni.createFrom().failure(new AuthenticationCompletionException(tOuter));
                        }

                        context.put(NEW_AUTHENTICATION, Boolean.TRUE);
                        context.put(OidcConstants.ACCESS_TOKEN_VALUE, tokens.getAccessToken());

                        return authenticate(identityProviderManager, context,
                                new IdTokenCredential(tokens.getIdToken(), context))
                                        .map(new Function<SecurityIdentity, SecurityIdentity>() {
                                            @Override
                                            public SecurityIdentity apply(SecurityIdentity identity) {
                                                processSuccessfulAuthentication(context, configContext,
                                                        tokens, identity);

                                                boolean removeRedirectParams = configContext.oidcConfig.authentication
                                                        .isRemoveRedirectParameters();
                                                if (removeRedirectParams || finalUserPath != null
                                                        || finalUserQuery != null) {

                                                    URI absoluteUri = URI.create(context.request().absoluteURI());

                                                    StringBuilder finalUriWithoutQuery = new StringBuilder(buildUri(context,
                                                            isForceHttps(configContext),
                                                            absoluteUri.getAuthority(),
                                                            (finalUserPath != null ? finalUserPath
                                                                    : absoluteUri.getRawPath())));

                                                    if (!removeRedirectParams) {
                                                        finalUriWithoutQuery.append('?').append(absoluteUri.getRawQuery());
                                                    }
                                                    if (finalUserQuery != null) {
                                                        finalUriWithoutQuery.append(!removeRedirectParams ? "" : "?");
                                                        finalUriWithoutQuery.append(finalUserQuery);
                                                    }
                                                    String finalRedirectUri = finalUriWithoutQuery.toString();
                                                    LOG.debugf("Final redirect URI: %s", finalRedirectUri);
                                                    throw new AuthenticationRedirectException(finalRedirectUri);
                                                } else {
                                                    return augmentIdentity(identity, tokens.getAccessToken(),
                                                            tokens.getRefreshToken(), context);
                                                }
                                            }
                                        }).onFailure().transform(new Function<Throwable, Throwable>() {
                                            @Override
                                            public Throwable apply(Throwable tInner) {
                                                if (tInner instanceof AuthenticationRedirectException) {
                                                    return tInner;
                                                }
                                                return new AuthenticationCompletionException(tInner);
                                            }
                                        });
                    }
                });

    }

    private void processSuccessfulAuthentication(RoutingContext context,
            TenantConfigContext configContext,
            AuthorizationCodeTokens tokens,
            SecurityIdentity securityIdentity) {
        removeCookie(context, configContext, getSessionCookieName(configContext.oidcConfig));

        JsonObject idToken = OidcUtils.decodeJwtContent(tokens.getIdToken());

        if (!idToken.containsKey("exp") || !idToken.containsKey("iat")) {
            LOG.debug("ID Token is required to contain 'exp' and 'iat' claims");
            throw new AuthenticationCompletionException();
        }
        long maxAge = idToken.getLong("exp") - idToken.getLong("iat");
        if (configContext.oidcConfig.token.lifespanGrace.isPresent()) {
            maxAge += configContext.oidcConfig.token.lifespanGrace.getAsInt();
        }
        if (configContext.oidcConfig.token.refreshExpired) {
            maxAge += configContext.oidcConfig.authentication.sessionAgeExtension.getSeconds();
        }
        context.put(SESSION_MAX_AGE_PARAM, maxAge);
        String cookieValue = resolver.getTokenStateManager()
                .createTokenState(context, configContext.oidcConfig, tokens);
        createCookie(context, configContext.oidcConfig, getSessionCookieName(configContext.oidcConfig), cookieValue, maxAge);

        fireEvent(SecurityEvent.Type.OIDC_LOGIN, securityIdentity);
    }

    private void fireEvent(SecurityEvent.Type eventType, SecurityIdentity securityIdentity) {
        if (resolver.isSecurityEventObserved()) {
            resolver.getSecurityEvent().fire(new SecurityEvent(eventType, securityIdentity));
        }
    }

    private String getRedirectPath(TenantConfigContext configContext, RoutingContext context) {
        Authentication auth = configContext.oidcConfig.getAuthentication();
        return auth.getRedirectPath().isPresent() ? auth.getRedirectPath().get() : context.request().path();
    }

    private String generateCodeFlowState(RoutingContext context, TenantConfigContext configContext,
            String redirectPath) {
        String uuid = UUID.randomUUID().toString();
        String cookieValue = uuid;

        Authentication auth = configContext.oidcConfig.getAuthentication();
        boolean restorePath = auth.isRestorePathAfterRedirect() || !auth.redirectPath.isPresent();
        if (restorePath) {
            String requestQuery = context.request().query();
            String requestPath = !redirectPath.equals(context.request().path()) || requestQuery != null
                    ? context.request().path()
                    : "";
            if (requestQuery != null) {
                requestPath += ("?" + requestQuery);
            }
            if (!requestPath.isEmpty()) {
                cookieValue += (COOKIE_DELIM + requestPath);
            }
        }
        createCookie(context, configContext.oidcConfig, getStateCookieName(configContext), cookieValue, 60 * 30);
        return uuid;
    }

    private String generatePostLogoutState(RoutingContext context, TenantConfigContext configContext) {
        removeCookie(context, configContext, getPostLogoutCookieName(configContext));
        return createCookie(context, configContext.oidcConfig, getPostLogoutCookieName(configContext),
                UUID.randomUUID().toString(),
                60 * 30).getValue();
    }

    static ServerCookie createCookie(RoutingContext context, OidcTenantConfig oidcConfig,
            String name, String value, long maxAge) {
        ServerCookie cookie = new CookieImpl(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(oidcConfig.authentication.cookieForceSecure || context.request().isSSL());
        cookie.setMaxAge(maxAge);
        LOG.debugf(name + " cookie 'max-age' parameter is set to %d", maxAge);
        Authentication auth = oidcConfig.getAuthentication();
        setCookiePath(context, auth, cookie);
        if (auth.cookieDomain.isPresent()) {
            cookie.setDomain(auth.getCookieDomain().get());
        }
        context.response().addCookie(cookie);
        return cookie;
    }

    static void setCookiePath(RoutingContext context, Authentication auth, ServerCookie cookie) {
        if (auth.cookiePathHeader.isPresent() && context.request().headers().contains(auth.cookiePathHeader.get())) {
            cookie.setPath(context.request().getHeader(auth.cookiePathHeader.get()));
        } else {
            cookie.setPath(auth.getCookiePath());
        }
    }

    private String buildUri(RoutingContext context, boolean forceHttps, String path) {
        String authority = URI.create(context.request().absoluteURI()).getAuthority();
        return buildUri(context, forceHttps, authority, path);
    }

    private String buildUri(RoutingContext context, boolean forceHttps, String authority, String path) {
        final String scheme = forceHttps ? "https" : context.request().scheme();
        String forwardedPrefix = "";
        if (resolver.isEnableHttpForwardedPrefix()) {
            String forwardedPrefixHeader = context.request().getHeader("X-Forwarded-Prefix");
            if (forwardedPrefixHeader != null && !forwardedPrefixHeader.equals("/") && !forwardedPrefixHeader.equals("//")) {
                forwardedPrefix = forwardedPrefixHeader;
                if (forwardedPrefix.endsWith("/")) {
                    forwardedPrefix = forwardedPrefix.substring(0, forwardedPrefix.length() - 1);
                }
            }
        }
        return new StringBuilder(scheme).append("://")
                .append(authority)
                .append(forwardedPrefix)
                .append(path)
                .toString();
    }

    private void removeCookie(RoutingContext context, TenantConfigContext configContext, String cookieName) {
        ServerCookie cookie = (ServerCookie) context.cookieMap().get(cookieName);
        if (cookie != null) {
            if (SESSION_COOKIE_NAME.equals(cookieName)) {
                resolver.getTokenStateManager().deleteTokens(context, configContext.oidcConfig, cookie.getValue());
            }
            removeCookie(context, cookie, configContext.oidcConfig);
        }
    }

    static void removeCookie(RoutingContext context, ServerCookie cookie, OidcTenantConfig oidcConfig) {
        if (cookie != null) {
            cookie.setValue("");
            cookie.setMaxAge(0);
            Authentication auth = oidcConfig.getAuthentication();
            setCookiePath(context, auth, cookie);
            if (auth.cookieDomain.isPresent()) {
                cookie.setDomain(auth.cookieDomain.get());
            }
        }
    }

    private boolean isLogout(RoutingContext context, TenantConfigContext configContext) {
        Optional<String> logoutPath = configContext.oidcConfig.logout.path;

        if (logoutPath.isPresent()) {
            return context.request().absoluteURI().equals(
                    buildUri(context, false, logoutPath.get()));
        }

        return false;
    }

    private Uni<SecurityIdentity> refreshSecurityIdentity(TenantConfigContext configContext, String refreshToken,
            RoutingContext context, IdentityProviderManager identityProviderManager, boolean autoRefresh,
            SecurityIdentity fallback) {

        Uni<AuthorizationCodeTokens> refreshedTokensUni = refreshTokensUni(configContext, refreshToken);

        return refreshedTokensUni
                .onItemOrFailure()
                .transformToUni(new BiFunction<AuthorizationCodeTokens, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(final AuthorizationCodeTokens tokens, final Throwable t) {
                        if (t != null) {
                            LOG.debugf("ID token refresh has failed: %s", t.getMessage());
                            if (autoRefresh) {
                                LOG.debug("Using the current SecurityIdentity since the ID token is still valid");
                                return Uni.createFrom().item(((TokenAutoRefreshException) t).getSecurityIdentity());
                            } else {
                                return Uni.createFrom().failure(new AuthenticationFailedException(t));
                            }
                        } else {
                            context.put(OidcConstants.ACCESS_TOKEN_VALUE, tokens.getAccessToken());
                            context.put(REFRESH_TOKEN_GRANT_RESPONSE, Boolean.TRUE);

                            return authenticate(identityProviderManager, context,
                                    new IdTokenCredential(tokens.getIdToken(), context))
                                            .map(new Function<SecurityIdentity, SecurityIdentity>() {
                                                @Override
                                                public SecurityIdentity apply(SecurityIdentity identity) {
                                                    // after a successful refresh, rebuild the identity and update the cookie
                                                    processSuccessfulAuthentication(context, configContext,
                                                            tokens, identity);
                                                    SecurityIdentity newSecurityIdentity = augmentIdentity(identity,
                                                            tokens.getAccessToken(), tokens.getRefreshToken(), context);

                                                    fireEvent(autoRefresh ? SecurityEvent.Type.OIDC_SESSION_REFRESHED
                                                            : SecurityEvent.Type.OIDC_SESSION_EXPIRED_AND_REFRESHED,
                                                            newSecurityIdentity);

                                                    return newSecurityIdentity;
                                                }
                                            }).onFailure().transform(new Function<Throwable, Throwable>() {
                                                @Override
                                                public Throwable apply(Throwable tInner) {
                                                    return new AuthenticationFailedException(tInner);
                                                }
                                            });
                        }
                    }
                });
    }

    private Uni<AuthorizationCodeTokens> refreshTokensUni(TenantConfigContext configContext, String refreshToken) {
        return configContext.provider.refreshTokens(refreshToken);
    }

    private Uni<AuthorizationCodeTokens> getCodeFlowTokensUni(RoutingContext context, TenantConfigContext configContext,
            String code) {

        // 'redirect_uri': typically it must match the 'redirect_uri' query parameter which was used during the code request.
        String redirectPath = getRedirectPath(configContext, context);
        String redirectUriParam = buildUri(context, isForceHttps(configContext), redirectPath);
        LOG.debugf("Token request redirect_uri parameter: %s", redirectUriParam);

        return configContext.provider.getCodeFlowTokens(code, redirectUriParam);
    }

    private String buildLogoutRedirectUri(TenantConfigContext configContext, String idToken, RoutingContext context) {
        String logoutPath = configContext.provider.getMetadata().getEndSessionUri();
        StringBuilder logoutUri = new StringBuilder(logoutPath).append("?").append("id_token_hint=").append(idToken);

        if (configContext.oidcConfig.logout.postLogoutPath.isPresent()) {
            logoutUri.append("&post_logout_redirect_uri=").append(
                    buildUri(context, isForceHttps(configContext), configContext.oidcConfig.logout.postLogoutPath.get()));
            logoutUri.append("&state=").append(generatePostLogoutState(context, configContext));
        }

        return logoutUri.toString();
    }

    private boolean isForceHttps(TenantConfigContext configContext) {
        return configContext.oidcConfig.authentication.forceRedirectHttpsScheme;
    }

    private AuthenticationRedirectException redirectToLogoutEndpoint(RoutingContext context, TenantConfigContext configContext,
            String idToken) {
        removeCookie(context, configContext, getSessionCookieName(configContext.oidcConfig));
        return new AuthenticationRedirectException(buildLogoutRedirectUri(configContext, idToken, context));
    }

    private static String getStateCookieName(TenantConfigContext configContext) {
        String cookieSuffix = getCookieSuffix(configContext.oidcConfig.tenantId.get());
        return STATE_COOKIE_NAME + cookieSuffix;
    }

    private static String getPostLogoutCookieName(TenantConfigContext configContext) {
        String cookieSuffix = getCookieSuffix(configContext.oidcConfig.tenantId.get());
        return POST_LOGOUT_COOKIE_NAME + cookieSuffix;
    }

    private static String getSessionCookieName(OidcTenantConfig oidcConfig) {
        String cookieSuffix = getCookieSuffix(oidcConfig.tenantId.get());
        return SESSION_COOKIE_NAME + cookieSuffix;
    }

    static String getCookieSuffix(String tenantId) {
        return !"Default".equals(tenantId) ? "_" + tenantId : "";
    }

}
