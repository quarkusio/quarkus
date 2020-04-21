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
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.AuthenticationCompletionException;
import io.quarkus.vertx.http.runtime.security.AuthenticationRedirectException;
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
    private static final String COOKIE_DELIM = "___";

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
        Cookie sessionCookie = context.request().getCookie(SESSION_COOKIE_NAME);
        TenantConfigContext configContext = resolver.resolve(context, true);

        // if session already established, try to re-authenticate
        if (sessionCookie != null) {
            String[] tokens = sessionCookie.getValue().split(COOKIE_DELIM);
            String idToken = tokens[0];
            String accessToken = tokens[1];
            String refreshToken = tokens[2];

            return authenticate(identityProviderManager, new IdTokenCredential(tokens[0], context))
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

                            Throwable cause = throwable.getCause();

                            // we should have proper exception hierarchy to represent token expiration errors
                            if (cause != null && !cause.getMessage().equalsIgnoreCase("expired token")) {
                                throw new AuthenticationCompletionException(throwable);
                            }

                            // try silent refresh if required
                            SecurityIdentity identity = null;

                            if (configContext.oidcConfig.token.refreshExpired) {
                                identity = trySilentRefresh(configContext, idToken, refreshToken, context,
                                        identityProviderManager);
                            }

                            if (identity == null) {
                                throw new AuthenticationFailedException(throwable);
                            }

                            return identity;
                        }
                    });
        }

        // start a new session by starting the code flow dance
        return performCodeFlow(identityProviderManager, context, resolver);
    }

    public Uni<ChallengeData> getChallenge(RoutingContext context, DefaultTenantConfigResolver resolver) {
        TenantConfigContext configContext = resolver.resolve(context, false);
        removeCookie(context, configContext, SESSION_COOKIE_NAME);

        ChallengeData challenge;
        JsonObject params = new JsonObject();

        // scope
        List<Object> scopes = new ArrayList<>();
        scopes.add("openid");
        configContext.oidcConfig.getAuthentication().scopes.ifPresent(scopes::addAll);
        params.put("scopes", new JsonArray(scopes));

        // redirect_uri
        String redirectPath = getRedirectPath(configContext, context);
        String redirectUriParam = buildUri(context, redirectPath);
        LOG.debugf("Authentication request redirect_uri parameter: %s", redirectUriParam);
        params.put("redirect_uri", redirectUriParam);

        // state
        params.put("state", generateState(context, configContext, redirectPath));

        // extra redirect parameters, see https://openid.net/specs/openid-connect-core-1_0.html#AuthRequests
        if (configContext.oidcConfig.authentication.getExtraParams() != null) {
            for (Map.Entry<String, String> entry : configContext.oidcConfig.authentication.getExtraParams().entrySet()) {
                params.put(entry.getKey(), entry.getValue());
            }
        }

        challenge = new ChallengeData(HttpResponseStatus.FOUND.code(), HttpHeaders.LOCATION,
                configContext.auth.authorizeURL(params));

        return Uni.createFrom().item(challenge);
    }

    private Uni<SecurityIdentity> performCodeFlow(IdentityProviderManager identityProviderManager,
            RoutingContext context, DefaultTenantConfigResolver resolver) {
        TenantConfigContext configContext = resolver.resolve(context, true);

        JsonObject params = new JsonObject();

        String code = context.request().getParam("code");
        if (code == null) {
            return Uni.createFrom().optional(Optional.empty());
        }

        Cookie stateCookie = context.getCookie(STATE_COOKIE_NAME);
        if (stateCookie != null) {
            List<String> values = context.queryParam("state");
            // IDP must return a 'state' query parameter and the value of the state cookie must start with this parameter's value
            if (values.size() != 1) {
                LOG.debug("State parameter can not be empty or multi-valued");
                return Uni.createFrom().failure(new AuthenticationCompletionException());
            } else if (!stateCookie.getValue().startsWith(values.get(0))) {
                LOG.debug("State cookie does not match the state parameter");
                return Uni.createFrom().failure(new AuthenticationCompletionException());
            } else if (context.queryParam("pathChecked").isEmpty()) {
                // This is an original redirect from IDP, check if the request path needs to be updated
                String[] pair = stateCookie.getValue().split(COOKIE_DELIM);
                if (pair.length == 2) {
                    // The extra path that needs to be added to the current request path
                    String extraPath = pair[1];
                    // Adding a query marker that the state cookie has already been used to restore the path
                    // as deleting it now would increase the risk of CSRF
                    String extraQuery = "?pathChecked=true";

                    // The query parameters returned from IDP need to be included
                    if (context.request().query() != null) {
                        extraQuery += ("&" + context.request().query());
                    }

                    String localRedirectUri = buildUri(context, extraPath + extraQuery);
                    LOG.debugf("Local redirect URI: %s", localRedirectUri);
                    return Uni.createFrom().failure(new AuthenticationRedirectException(localRedirectUri));
                }
                // The original request path does not have to be restored, the state cookie is no longer needed
                removeCookie(context, configContext, STATE_COOKIE_NAME);
            } else {
                // Local redirect restoring the original request path, the state cookie is no longer needed
                removeCookie(context, configContext, STATE_COOKIE_NAME);
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
        String redirectUriParam = buildUri(context, redirectPath);
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
                        AccessToken result = AccessToken.class.cast(userAsyncResult.result());

                        authenticate(identityProviderManager, new IdTokenCredential(result.opaqueIdToken(), context))
                                .subscribe().with(new Consumer<SecurityIdentity>() {
                                    @Override
                                    public void accept(SecurityIdentity identity) {
                                        if (!result.idToken().containsKey("exp") || !result.idToken().containsKey("iat")) {
                                            LOG.debug("ID Token is required to contain 'exp' and 'iat' claims");
                                            uniEmitter.fail(new AuthenticationCompletionException());
                                        }
                                        processSuccessfulAuthentication(context, configContext, result, identity);
                                        uniEmitter.complete(augmentIdentity(identity, result.opaqueAccessToken(),
                                                result.opaqueRefreshToken(), context));
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
            AccessToken result, SecurityIdentity securityIdentity) {
        removeCookie(context, configContext, SESSION_COOKIE_NAME);

        CookieImpl cookie = new CookieImpl(SESSION_COOKIE_NAME, new StringBuilder(result.opaqueIdToken())
                .append(COOKIE_DELIM)
                .append(result.opaqueAccessToken())
                .append(COOKIE_DELIM)
                .append(result.opaqueRefreshToken()).toString());
        long maxAge = result.idToken().getLong("exp") - result.idToken().getLong("iat");
        if (configContext.oidcConfig.token.lifespanGrace.isPresent()) {
            maxAge += configContext.oidcConfig.token.lifespanGrace.get();
        }
        LOG.debugf("Session cookie 'max-age' parameter is set to %d", maxAge);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(context.request().isSSL());
        cookie.setHttpOnly(true);
        if (configContext.oidcConfig.authentication.cookiePath.isPresent()) {
            cookie.setPath(configContext.oidcConfig.authentication.cookiePath.get());
        }
        context.response().addCookie(cookie);
    }

    private String getRedirectPath(TenantConfigContext configContext, RoutingContext context) {
        Authentication auth = configContext.oidcConfig.getAuthentication();
        return auth.getRedirectPath().isPresent() ? auth.getRedirectPath().get() : context.request().path();
    }

    private String generateState(RoutingContext context, TenantConfigContext configContext,
            String redirectPath) {
        String uuid = UUID.randomUUID().toString();
        String cookieValue = uuid;

        Authentication auth = configContext.oidcConfig.getAuthentication();
        if (auth.isRestorePathAfterRedirect() && !redirectPath.equals(context.request().path())) {
            cookieValue += (COOKIE_DELIM + context.request().path());
        }

        CookieImpl cookie = new CookieImpl(STATE_COOKIE_NAME, cookieValue);

        cookie.setHttpOnly(true);
        cookie.setSecure(context.request().isSSL());
        // max-age is 30 minutes
        cookie.setMaxAge(60 * 30);
        if (auth.cookiePath.isPresent()) {
            cookie.setPath(auth.getCookiePath().get());
        }
        context.response().addCookie(cookie);
        return uuid;
    }

    private String buildUri(RoutingContext context, String path) {
        return new StringBuilder(context.request().scheme()).append("://")
                .append(URI.create(context.request().absoluteURI()).getAuthority())
                .append(path)
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
                    buildUri(context, logoutPath.get()));
        }

        return false;
    }

    private SecurityIdentity trySilentRefresh(TenantConfigContext configContext, String idToken, String refreshToken,
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
                            authenticate(identityProviderManager,
                                    new IdTokenCredential(token.opaqueIdToken(), context))
                                            .subscribe().with(new Consumer<SecurityIdentity>() {
                                                @Override
                                                public void accept(SecurityIdentity identity) {
                                                    // after a successful refresh, rebuild the identity and update the cookie 
                                                    processSuccessfulAuthentication(context, configContext, token,
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
                    buildUri(context, configContext.oidcConfig.logout.postLogoutPath.get()));
        }

        return logoutUri.toString();
    }

    private AuthenticationRedirectException redirectToLogoutEndpoint(RoutingContext context, TenantConfigContext configContext,
            String idToken) {
        removeCookie(context, configContext, SESSION_COOKIE_NAME);
        return new AuthenticationRedirectException(buildLogoutRedirectUri(configContext, idToken, context));
    }
}
