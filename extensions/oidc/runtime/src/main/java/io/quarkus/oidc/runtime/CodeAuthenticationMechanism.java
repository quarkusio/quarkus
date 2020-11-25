package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcIdentityProvider.CODE_FLOW_ACCESS_TOKEN;
import static io.quarkus.oidc.runtime.OidcIdentityProvider.NEW_AUTHENTICATION;
import static io.quarkus.oidc.runtime.OidcIdentityProvider.REFRESH_TOKEN_GRANT_RESPONSE;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Authentication;
import io.quarkus.oidc.OidcTenantConfig.Credentials;
import io.quarkus.oidc.OidcTenantConfig.Credentials.Secret;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.auth.oauth2.impl.OAuth2TokenImpl;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.CookieImpl;

public class CodeAuthenticationMechanism extends AbstractOidcAuthenticationMechanism {

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

        context.put(CODE_FLOW_ACCESS_TOKEN, session.getAccessToken());
        return authenticate(identityProviderManager, new IdTokenCredential(session.getIdToken(), context))
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
                    public Uni<? extends SecurityIdentity> apply(Throwable throwable) {
                        if (throwable instanceof AuthenticationRedirectException) {
                            throw AuthenticationRedirectException.class.cast(throwable);
                        }

                        if (!(throwable instanceof TokenAutoRefreshException)) {
                            Throwable cause = throwable.getCause();

                            if (cause != null && !"expired token".equalsIgnoreCase(cause.getMessage())) {
                                LOG.debugf("Authentication failure: %s", cause);
                                throw new AuthenticationCompletionException(cause);
                            }
                            if (!configContext.oidcConfig.token.refreshExpired) {
                                LOG.debug("Token has expired, token refresh is not allowed");
                                throw new AuthenticationCompletionException(cause);
                            }
                            LOG.debug("Token has expired, trying to refresh it");
                            return refreshSecurityIdentity(configContext, session.getRefreshToken(), context,
                                    identityProviderManager, false, null);
                        } else {
                            return refreshSecurityIdentity(configContext, session.getRefreshToken(), context,
                                    identityProviderManager, true,
                                    ((TokenAutoRefreshException) throwable).getSecurityIdentity());
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

        JsonObject params = new JsonObject();

        // scope
        List<Object> scopes = new ArrayList<>();
        scopes.add("openid");
        configContext.oidcConfig.getAuthentication().scopes.ifPresent(scopes::addAll);
        params.put("scopes", new JsonArray(scopes));

        // redirect_uri
        String redirectPath = getRedirectPath(configContext, context);
        String redirectUriParam = buildUri(context, isForceHttps(configContext), redirectPath);
        LOG.debugf("Authentication request redirect_uri parameter: %s", redirectUriParam);
        params.put("redirect_uri", redirectUriParam);

        // state
        params.put("state", generateCodeFlowState(context, configContext, redirectPath));

        // extra redirect parameters, see https://openid.net/specs/openid-connect-core-1_0.html#AuthRequests
        if (configContext.oidcConfig.authentication.getExtraParams() != null) {
            for (Map.Entry<String, String> entry : configContext.oidcConfig.authentication.getExtraParams().entrySet()) {
                params.put(entry.getKey(), entry.getValue());
            }
        }

        return Uni.createFrom().item(new ChallengeData(HttpResponseStatus.FOUND.code(), HttpHeaders.LOCATION,
                configContext.auth.authorizeURL(params)));
    }

    private Uni<SecurityIdentity> performCodeFlow(IdentityProviderManager identityProviderManager,
            RoutingContext context, TenantConfigContext configContext, String code) {

        context.put(NEW_AUTHENTICATION, Boolean.TRUE);

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

        // Code grant request
        JsonObject params = new JsonObject();

        // 'code': the code grant value returned from IDP
        params.put("code", code);

        // 'redirect_uri': typically it must match the 'redirect_uri' query parameter which was used during the code request.
        String redirectPath = getRedirectPath(configContext, context);
        String redirectUriParam = buildUri(context, isForceHttps(configContext), redirectPath);
        LOG.debugf("Token request redirect_uri parameter: %s", redirectUriParam);
        params.put("redirect_uri", redirectUriParam);

        // Client secret has to be posted as a form parameter if OIDC requires the client_secret_post authentication
        Credentials creds = configContext.oidcConfig.getCredentials();
        if (creds.clientSecret.value.isPresent() && Secret.Method.POST == creds.clientSecret.method.orElse(null)) {
            params.put("client_secret", creds.clientSecret.value.get());
        } else if (creds.jwt.secret.isPresent()) {
            params.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
            params.put("client_assertion", signJwtWithClientSecret(configContext.oidcConfig));
        }

        final String finalUserPath = userPath;
        final String finalUserQuery = userQuery;
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
            @Override
            public void accept(UniEmitter<? super SecurityIdentity> uniEmitter) {
                configContext.auth.authenticate(params, userAsyncResult -> {
                    if (userAsyncResult.failed()) {
                        if (userAsyncResult.cause() != null) {
                            LOG.debugf("Exception during the code to token exchange: %s", userAsyncResult.cause().getMessage());
                        }
                        uniEmitter.fail(new AuthenticationCompletionException(userAsyncResult.cause()));
                    } else {
                        final AccessToken authResult = AccessToken.class.cast(userAsyncResult.result());
                        final String opaqueIdToken = authResult.opaqueIdToken();
                        final String opaqueAccessToken = authResult.opaqueAccessToken();
                        final String opaqueRefreshToken = authResult.opaqueRefreshToken();
                        context.put(CODE_FLOW_ACCESS_TOKEN, opaqueAccessToken);
                        authenticate(identityProviderManager, new IdTokenCredential(opaqueIdToken, context))
                                .subscribe().with(new Consumer<SecurityIdentity>() {
                                    @Override
                                    public void accept(SecurityIdentity identity) {
                                        processSuccessfulAuthentication(context, configContext, authResult.idToken(),
                                                opaqueIdToken, opaqueAccessToken, opaqueRefreshToken, identity);

                                        boolean removeRedirectParams = configContext.oidcConfig.authentication
                                                .isRemoveRedirectParameters();
                                        if (removeRedirectParams || finalUserPath != null || finalUserQuery != null) {

                                            final String scheme = isForceHttps(configContext) ? "https"
                                                    : context.request().scheme();
                                            URI absoluteUri = URI.create(context.request().absoluteURI());
                                            StringBuilder sb = new StringBuilder(scheme).append("://")
                                                    .append(absoluteUri.getAuthority())
                                                    .append(finalUserPath != null ? finalUserPath : absoluteUri.getRawPath());
                                            if (!removeRedirectParams) {
                                                sb.append('?').append(absoluteUri.getRawQuery());
                                            }
                                            if (finalUserQuery != null) {
                                                sb.append(!removeRedirectParams ? "" : "?");
                                                sb.append(finalUserQuery);
                                            }
                                            String finalRedirectUri = sb.toString();
                                            LOG.debugf("Final redirect URI: %s", finalRedirectUri);
                                            uniEmitter.fail(new AuthenticationRedirectException(finalRedirectUri));
                                        } else {
                                            uniEmitter.complete(augmentIdentity(identity, opaqueAccessToken,
                                                    opaqueRefreshToken, context));
                                        }
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) {
                                        uniEmitter.fail(throwable);
                                    }
                                });
                    }
                });
            }
        });
    }

    private String signJwtWithClientSecret(OidcTenantConfig cfg) {
        final byte[] keyBytes = cfg.credentials.jwt.secret.get().getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HMACSHA256");

        // 'jti' claim is created by default.
        final long iat = (System.currentTimeMillis() / 1000);
        final long exp = iat + cfg.credentials.jwt.lifespan;

        return Jwt.claims()
                .issuer(cfg.clientId.get())
                .subject(cfg.clientId.get())
                .audience(cfg.authServerUrl.get())
                .issuedAt(iat)
                .expiresAt(exp)
                .sign(key);
    }

    private void processSuccessfulAuthentication(RoutingContext context,
            TenantConfigContext configContext,
            JsonObject idToken,
            String opaqueIdToken,
            String opaqueAccessToken,
            String opaqueRefreshToken,
            SecurityIdentity securityIdentity) {
        removeCookie(context, configContext, getSessionCookieName(configContext.oidcConfig));

        if (idToken == null) {
            // it can be null if Vert.x did the remote introspection of the ID token
            idToken = OidcUtils.decodeJwtContent(opaqueIdToken);
        }
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
                .createTokenState(context, configContext.oidcConfig,
                        new AuthorizationCodeTokens(opaqueIdToken, opaqueAccessToken, opaqueRefreshToken));
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
        if (auth.isRestorePathAfterRedirect()) {
            String requestPath = !redirectPath.equals(context.request().path()) ? context.request().path() : "";
            if (context.request().query() != null) {
                requestPath += ("?" + context.request().query());
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

    static CookieImpl createCookie(RoutingContext context, OidcTenantConfig oidcConfig,
            String name, String value, long maxAge) {
        CookieImpl cookie = new CookieImpl(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(context.request().isSSL());
        cookie.setMaxAge(maxAge);
        LOG.debugf(name + " cookie 'max-age' parameter is set to %d", maxAge);
        Authentication auth = oidcConfig.getAuthentication();
        if (auth.cookiePath.isPresent()) {
            cookie.setPath(auth.getCookiePath().get());
        }
        if (auth.cookieDomain.isPresent()) {
            cookie.setDomain(auth.getCookieDomain().get());
        }
        context.response().addCookie(cookie);
        return cookie;
    }

    private String buildUri(RoutingContext context, boolean forceHttps, String path) {
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
                .append(URI.create(context.request().absoluteURI()).getAuthority())
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

            cookie.setValue("");
            cookie.setMaxAge(0);
            Authentication auth = configContext.oidcConfig.getAuthentication();
            if (auth.cookiePath.isPresent()) {
                cookie.setPath(auth.cookiePath.get());
            }
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

        Uni<OAuth2TokenImpl> refreshedTokensUni = trySilentRefreshUni(configContext, refreshToken);

        return refreshedTokensUni
                .onItemOrFailure()
                .transformToUni(new BiFunction<OAuth2TokenImpl, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(final OAuth2TokenImpl token, final Throwable t) {
                        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
                            @Override
                            public void accept(UniEmitter<? super SecurityIdentity> emitter) {
                                if (t != null) {
                                    LOG.debugf("ID token refresh has failed: %s", t.getMessage());

                                    if (autoRefresh) {
                                        LOG.debug("Using the current SecurityIdentity since the ID token is still valid");
                                        emitter.complete(((TokenAutoRefreshException) t).getSecurityIdentity());
                                    } else {
                                        emitter.fail(new AuthenticationFailedException(t));
                                    }
                                } else {
                                    // Verify the refreshed tokens
                                    final String opaqueIdToken = token.opaqueIdToken();
                                    final String opaqueAccessToken = token.opaqueAccessToken();
                                    final String opaqueRefreshToken = token.opaqueRefreshToken();
                                    context.put(CODE_FLOW_ACCESS_TOKEN, token.opaqueAccessToken());
                                    context.put(REFRESH_TOKEN_GRANT_RESPONSE, Boolean.TRUE);
                                    authenticate(identityProviderManager,
                                            new IdTokenCredential(token.opaqueIdToken(), context))
                                                    .subscribe().with(new Consumer<SecurityIdentity>() {
                                                        @Override
                                                        public void accept(SecurityIdentity identity) {
                                                            // the refresh token might not have been sent in the response again
                                                            String refresh = opaqueRefreshToken != null
                                                                    ? opaqueRefreshToken
                                                                    : refreshToken;
                                                            // after a successful refresh, rebuild the identity and update the cookie
                                                            processSuccessfulAuthentication(context, configContext,
                                                                    token.idToken(),
                                                                    opaqueIdToken, opaqueAccessToken, refresh, identity);
                                                            SecurityIdentity newSecurityIdentity = augmentIdentity(identity,
                                                                    opaqueAccessToken, refresh, context);

                                                            fireEvent(autoRefresh ? SecurityEvent.Type.OIDC_SESSION_REFRESHED
                                                                    : SecurityEvent.Type.OIDC_SESSION_EXPIRED_AND_REFRESHED,
                                                                    newSecurityIdentity);

                                                            emitter.complete(newSecurityIdentity);
                                                        }
                                                    }, new Consumer<Throwable>() {
                                                        @Override
                                                        public void accept(Throwable throwable) {
                                                            emitter.fail(new AuthenticationFailedException(throwable));
                                                        }
                                                    });
                                }
                            }
                        });
                    }
                });
    }

    private Uni<OAuth2TokenImpl> trySilentRefreshUni(TenantConfigContext configContext, String refreshToken) {

        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super OAuth2TokenImpl>>() {
            @Override
            public void accept(UniEmitter<? super OAuth2TokenImpl> emitter) {
                if (BlockingOperationControl.isBlockingAllowed()) {
                    trySilentRefresh(emitter, configContext, refreshToken);
                } else {
                    resolver.getBlockingExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            trySilentRefresh(emitter, configContext, refreshToken);
                        }
                    });
                }
            }
        });
    }

    private void trySilentRefresh(UniEmitter<? super OAuth2TokenImpl> emitter, TenantConfigContext configContext,
            String refreshToken) {

        final OAuth2TokenImpl token = new OAuth2TokenImpl(configContext.auth, new JsonObject());

        // always get the last token
        token.principal().put("refresh_token", refreshToken);

        token.refresh(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> result) {
                if (result.succeeded()) {
                    emitter.complete(token);
                } else {
                    emitter.fail(new AuthenticationFailedException(result.cause()));
                }
            }
        });
    }

    private String buildLogoutRedirectUri(TenantConfigContext configContext, String idToken, RoutingContext context) {
        String logoutPath = configContext.oidcConfig.getEndSessionPath()
                .orElse(OAuth2AuthProviderImpl.class.cast(configContext.auth).getConfig().getLogoutPath());
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
