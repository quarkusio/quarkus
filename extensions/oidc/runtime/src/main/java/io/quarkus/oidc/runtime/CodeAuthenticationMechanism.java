package io.quarkus.oidc.runtime;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Authentication;
import io.quarkus.oidc.OidcTenantConfig.Credentials;
import io.quarkus.oidc.OidcTenantConfig.Credentials.Secret;
import io.quarkus.oidc.RefreshToken;
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

    private static final Logger LOG = Logger.getLogger(CodeAuthenticationMechanism.class);

    private static final String STATE_COOKIE_NAME = "q_auth";
    private static final String SESSION_COOKIE_NAME = "q_session";
    private static final String POST_LOGOUT_COOKIE_NAME = "q_post_logout";
    private static final String COOKIE_DELIM = "|";
    private static final Pattern COOKIE_PATTERN = Pattern.compile("\\" + COOKIE_DELIM);

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
            IdentityProviderManager identityProviderManager,
            DefaultTenantConfigResolver resolver) {

        Cookie sessionCookie = context.request().getCookie(
                getSessionCookieName(resolver.resolve(context, false)));

        // if session already established, try to re-authenticate
        if (sessionCookie != null) {
            String[] tokens = COOKIE_PATTERN.split(sessionCookie.getValue());
            String idToken = tokens[0];
            String accessToken = tokens[1];
            String refreshToken = tokens[2];

            TenantConfigContext configContext = resolver.resolve(context, true);
            context.put("access_token", accessToken);
            return authenticate(identityProviderManager, new IdTokenCredential(idToken, context))
                    .map(new Function<SecurityIdentity, SecurityIdentity>() {
                        @Override
                        public SecurityIdentity apply(SecurityIdentity identity) {
                            if (isLogout(context, configContext)) {
                                throw redirectToLogoutEndpoint(context, configContext, idToken);
                            }

                            return augmentIdentity(identity, accessToken, refreshToken, context);
                        }
                    }).on().failure().recoverWithItem(new Function<Throwable, SecurityIdentity>() {
                        @Override
                        public SecurityIdentity apply(Throwable throwable) {
                            if (throwable instanceof AuthenticationRedirectException) {
                                throw AuthenticationRedirectException.class.cast(throwable);
                            }

                            SecurityIdentity identity = null;

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
                                identity = trySilentRefresh(configContext, refreshToken, context, identityProviderManager);
                                if (identity == null) {
                                    LOG.debug("SecurityIdentity is null after a token refresh");
                                    throw new AuthenticationCompletionException();
                                }
                            } else {
                                identity = trySilentRefresh(configContext, refreshToken, context, identityProviderManager);
                                if (identity == null) {
                                    LOG.debug("ID token can no longer be refreshed, using the current SecurityIdentity");
                                    identity = ((TokenAutoRefreshException) throwable).getSecurityIdentity();
                                }
                            }
                            return identity;
                        }
                    });
        }

        // start a new session by starting the code flow dance
        context.put("new_authentication", Boolean.TRUE);
        return performCodeFlow(identityProviderManager, context, resolver);
    }

    private boolean isXHR(RoutingContext context) {
        return "XMLHttpRequest".equals(context.request().getHeader("X-Requested-With"));
    }

    // This test determines if the default behavior of returning a 302 should go forward
    // The only case that shouldn't return a 302 is if the call is a XHR and the 
    // user has set the auto direct application property to false indicating that
    // the client application will manually handle the redirect to account for SPA behavior
    private boolean shouldAutoRedirect(TenantConfigContext configContext, RoutingContext context) {
        return isXHR(context) ? configContext.oidcConfig.authentication.xhrAutoRedirect : true;
    }

    public Uni<ChallengeData> getChallenge(RoutingContext context, DefaultTenantConfigResolver resolver) {

        TenantConfigContext configContext = resolver.resolve(context, true);
        removeCookie(context, configContext, getSessionCookieName(configContext));

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
            RoutingContext context, DefaultTenantConfigResolver resolver) {
        JsonObject params = new JsonObject();

        String code = context.request().getParam("code");
        if (code == null) {
            return Uni.createFrom().optional(Optional.empty());
        }

        TenantConfigContext configContext = resolver.resolve(context, true);
        Cookie stateCookie = context.getCookie(getStateCookieName(configContext));

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
            } else if (context.queryParam("pathChecked").isEmpty()) {
                // This is an original redirect from IDP, check if the request path needs to be updated
                String[] pair = COOKIE_PATTERN.split(stateCookie.getValue());
                if (pair.length == 2) {
                    // The extra path that needs to be added to the current request path
                    String extraPath = pair[1];

                    // The original user query if any will be added to the final redirect URI
                    // after the authentication has been complete

                    int userQueryIndex = extraPath.indexOf("?");
                    if (userQueryIndex != 0) {
                        if (userQueryIndex > 0) {
                            extraPath = extraPath.substring(0, userQueryIndex);
                        }
                        // Adding a query marker that the state cookie has already been used to restore the path
                        // as deleting it now would increase the risk of CSRF
                        String extraQuery = "?pathChecked=true";

                        // The query parameters returned from IDP need to be included
                        if (context.request().query() != null) {
                            extraQuery += ("&" + context.request().query());
                        }

                        String localRedirectUri = buildUri(context, isForceHttps(configContext), extraPath + extraQuery);
                        LOG.debugf("Local redirect URI: %s", localRedirectUri);
                        return Uni.createFrom().failure(new AuthenticationRedirectException(localRedirectUri));
                    } else if (userQueryIndex + 1 < extraPath.length()) {
                        // only the user query needs to be restored, no need to redirect
                        userQuery = extraPath.substring(userQueryIndex + 1);
                    }
                }
                // The original request path does not have to be restored, the state cookie is no longer needed
                removeCookie(context, configContext, getStateCookieName(configContext));
            } else {
                String[] pair = COOKIE_PATTERN.split(stateCookie.getValue());
                if (pair.length == 2) {
                    int userQueryIndex = pair[1].indexOf("?");
                    if (userQueryIndex >= 0 && userQueryIndex + 1 < pair[1].length()) {
                        userQuery = pair[1].substring(userQueryIndex + 1);
                    }
                }
                // Local redirect restoring the original request path, the state cookie is no longer needed
                removeCookie(context, configContext, getStateCookieName(configContext));
            }
        } else {
            // State cookie must be available to minimize the risk of CSRF
            LOG.debug("The state cookie is missing after a redirect from IDP");
            return Uni.createFrom().failure(new AuthenticationCompletionException());
        }

        // Code grant request
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
                        final AccessToken result = AccessToken.class.cast(userAsyncResult.result());

                        context.put("access_token", result.opaqueAccessToken());
                        authenticate(identityProviderManager, new IdTokenCredential(result.opaqueIdToken(), context))
                                .subscribe().with(new Consumer<SecurityIdentity>() {
                                    @Override
                                    public void accept(SecurityIdentity identity) {
                                        if (!result.idToken().containsKey("exp") || !result.idToken().containsKey("iat")) {
                                            LOG.debug("ID Token is required to contain 'exp' and 'iat' claims");
                                            uniEmitter.fail(new AuthenticationCompletionException());
                                        }
                                        processSuccessfulAuthentication(context, configContext, result,
                                                result.opaqueRefreshToken(), identity);

                                        if (configContext.oidcConfig.authentication.isRemoveRedirectParameters()
                                                && context.request().query() != null) {
                                            String finalRedirectUri = buildUriWithoutQueryParams(context,
                                                    isForceHttps(configContext));
                                            if (finalUserQuery != null) {
                                                finalRedirectUri += ("?" + finalUserQuery);
                                            }
                                            LOG.debugf("Final redirect URI: %s", finalRedirectUri);
                                            uniEmitter.fail(new AuthenticationRedirectException(finalRedirectUri));
                                        } else {
                                            uniEmitter.complete(augmentIdentity(identity, result.opaqueAccessToken(),
                                                    result.opaqueRefreshToken(), context));
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

    private void processSuccessfulAuthentication(RoutingContext context, TenantConfigContext configContext,
            AccessToken result, String refreshToken, SecurityIdentity securityIdentity) {
        removeCookie(context, configContext, getSessionCookieName(configContext));

        String cookieValue = result.opaqueIdToken() + COOKIE_DELIM
                + result.opaqueAccessToken() + COOKIE_DELIM
                + refreshToken;

        long maxAge = result.idToken().getLong("exp") - result.idToken().getLong("iat");
        if (configContext.oidcConfig.token.lifespanGrace.isPresent()) {
            maxAge += configContext.oidcConfig.token.lifespanGrace.getAsInt();
        }
        if (configContext.oidcConfig.token.refreshExpired) {
            maxAge += configContext.oidcConfig.authentication.sessionAgeExtension.getSeconds();
        }
        createCookie(context, configContext, getSessionCookieName(configContext), cookieValue, maxAge);
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
        createCookie(context, configContext, getStateCookieName(configContext), cookieValue, 60 * 30);
        return uuid;
    }

    private String generatePostLogoutState(RoutingContext context, TenantConfigContext configContext) {
        removeCookie(context, configContext, getPostLogoutCookieName(configContext));
        return createCookie(context, configContext, getPostLogoutCookieName(configContext), UUID.randomUUID().toString(),
                60 * 30).getValue();
    }

    private CookieImpl createCookie(RoutingContext context, TenantConfigContext configContext,
            String name, String value, long maxAge) {
        CookieImpl cookie = new CookieImpl(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(context.request().isSSL());
        cookie.setMaxAge(maxAge);
        LOG.debugf(name + " cookie 'max-age' parameter is set to %d", maxAge);
        Authentication auth = configContext.oidcConfig.getAuthentication();
        if (auth.cookiePath.isPresent()) {
            cookie.setPath(auth.getCookiePath().get());
        }
        context.response().addCookie(cookie);
        return cookie;
    }

    private String buildUri(RoutingContext context, boolean forceHttps, String path) {
        final String scheme = forceHttps ? "https" : context.request().scheme();
        return new StringBuilder(scheme).append("://")
                .append(URI.create(context.request().absoluteURI()).getAuthority())
                .append(path)
                .toString();
    }

    private String buildUriWithoutQueryParams(RoutingContext context, boolean forceHttps) {
        final String scheme = forceHttps ? "https" : context.request().scheme();
        URI absoluteUri = URI.create(context.request().absoluteURI());
        return new StringBuilder(scheme).append("://")
                .append(absoluteUri.getAuthority())
                .append(absoluteUri.getRawPath())
                .toString();
    }

    private void removeCookie(RoutingContext context, TenantConfigContext configContext, String cookieName) {
        ServerCookie cookie = (ServerCookie) context.cookieMap().get(cookieName);
        if (cookie != null) {
            cookie.setValue("");
            cookie.setMaxAge(0);
            Authentication auth = configContext.oidcConfig.getAuthentication();
            if (auth.cookiePath.isPresent()) {
                cookie.setPath(auth.cookiePath.get());
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

    private SecurityIdentity trySilentRefresh(TenantConfigContext configContext, String refreshToken,
            RoutingContext context, IdentityProviderManager identityProviderManager) {

        Uni<SecurityIdentity> cf = Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
            @Override
            public void accept(UniEmitter<? super SecurityIdentity> emitter) {
                OAuth2TokenImpl token = new OAuth2TokenImpl(configContext.auth, new JsonObject());

                // always get the last token
                token.principal().put("refresh_token", refreshToken);

                token.refresh(new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> result) {
                        if (result.succeeded()) {
                            context.put("access_token", token.opaqueAccessToken());
                            authenticate(identityProviderManager,
                                    new IdTokenCredential(token.opaqueIdToken(), context))
                                            .subscribe().with(new Consumer<SecurityIdentity>() {
                                                @Override
                                                public void accept(SecurityIdentity identity) {
                                                    // the refresh token might not have been send in the response again
                                                    String refresh = token.opaqueRefreshToken() != null
                                                            ? token.opaqueRefreshToken()
                                                            : refreshToken;
                                                    // after a successful refresh, rebuild the identity and update the cookie
                                                    processSuccessfulAuthentication(context, configContext, token, refresh,
                                                            identity);
                                                    // update the token so that blocking threads get the latest one
                                                    emitter.complete(
                                                            augmentIdentity(identity, token.opaqueAccessToken(),
                                                                    token.opaqueRefreshToken(),
                                                                    context));
                                                }
                                            }, new Consumer<Throwable>() {
                                                @Override
                                                public void accept(Throwable throwable) {
                                                    emitter.fail(throwable);
                                                }
                                            });
                        } else {
                            emitter.fail(new AuthenticationFailedException(result.cause()));
                        }
                    }
                });
            }
        });

        return cf.await().indefinitely();
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
        removeCookie(context, configContext, getSessionCookieName(configContext));
        return new AuthenticationRedirectException(buildLogoutRedirectUri(configContext, idToken, context));
    }

    private static String getSessionCookieName(TenantConfigContext configContext) {
        String cookieSuffix = getCookieSuffix(configContext);
        return SESSION_COOKIE_NAME + cookieSuffix;
    }

    private static String getStateCookieName(TenantConfigContext configContext) {
        String cookieSuffix = getCookieSuffix(configContext);
        return STATE_COOKIE_NAME + cookieSuffix;
    }

    private static String getPostLogoutCookieName(TenantConfigContext configContext) {
        String cookieSuffix = getCookieSuffix(configContext);
        return POST_LOGOUT_COOKIE_NAME + cookieSuffix;
    }

    private static String getCookieSuffix(TenantConfigContext configContext) {
        return !"Default".equals(configContext.oidcConfig.tenantId.get()) ? "_" + configContext.oidcConfig.tenantId.get() : "";
    }
}
