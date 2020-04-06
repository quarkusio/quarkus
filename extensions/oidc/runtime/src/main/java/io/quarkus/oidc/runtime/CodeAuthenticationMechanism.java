package io.quarkus.oidc.runtime;

import java.net.URI;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.oidc.runtime.OidcTenantConfig.Authentication;
import io.quarkus.oidc.runtime.OidcTenantConfig.Credentials;
import io.quarkus.oidc.runtime.OidcTenantConfig.Credentials.Secret;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.AuthenticationCompletionException;
import io.quarkus.vertx.http.runtime.security.AuthenticationRedirectException;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
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
        final RefreshToken refreshTokenCredential = new RefreshToken(refreshToken);
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(securityIdentity.getPrincipal())
                .addCredentials(securityIdentity.getCredentials())
                .addCredential(new AccessTokenCredential(accessToken, refreshTokenCredential, context))
                .addCredential(refreshTokenCredential)
                .addRoles(securityIdentity.getRoles())
                .addAttributes(securityIdentity.getAttributes())
                .addPermissionChecker(new Function<Permission, CompletionStage<Boolean>>() {
                    @Override
                    public CompletionStage<Boolean> apply(Permission permission) {
                        return securityIdentity.checkPermission(permission);
                    }
                })
                .build();
    }

    public CompletionStage<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager,
            DefaultTenantConfigResolver resolver) {
        Cookie sessionCookie = context.request().getCookie(SESSION_COOKIE_NAME);

        // if session already established, try to re-authenticate
        if (sessionCookie != null) {
            String[] tokens = sessionCookie.getValue().split(COOKIE_DELIM);
            return authenticate(identityProviderManager, new IdTokenCredential(tokens[0], context))
                    .thenCompose(new Function<SecurityIdentity, CompletionStage<SecurityIdentity>>() {
                        @Override
                        public CompletionStage<SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                            return CompletableFuture
                                    .completedFuture(augmentIdentity(securityIdentity, tokens[1], tokens[2], context));
                        }
                    });
        }

        // start a new session by starting the code flow dance
        return performCodeFlow(identityProviderManager, context, resolver);
    }

    public CompletionStage<ChallengeData> getChallenge(RoutingContext context, DefaultTenantConfigResolver resolver) {
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
        URI absoluteUri = URI.create(context.request().absoluteURI());
        String redirectPath = getRedirectPath(configContext, absoluteUri);
        String redirectUriParam = buildRedirectUri(context, absoluteUri, redirectPath);
        LOG.debugf("Authentication request redirect_uri parameter: %s", redirectUriParam);
        params.put("redirect_uri", redirectUriParam);

        // state
        params.put("state", generateState(context, configContext, absoluteUri, redirectPath));

        // extra redirect parameters, see https://openid.net/specs/openid-connect-core-1_0.html#AuthRequests
        if (configContext.oidcConfig.authentication.getExtraParams() != null) {
            for (Map.Entry<String, String> entry : configContext.oidcConfig.authentication.getExtraParams().entrySet()) {
                params.put(entry.getKey(), entry.getValue());
            }
        }

        challenge = new ChallengeData(HttpResponseStatus.FOUND.code(), HttpHeaders.LOCATION,
                configContext.auth.authorizeURL(params));

        return CompletableFuture.completedFuture(challenge);
    }

    private CompletionStage<SecurityIdentity> performCodeFlow(IdentityProviderManager identityProviderManager,
            RoutingContext context, DefaultTenantConfigResolver resolver) {
        TenantConfigContext configContext = resolver.resolve(context, true);

        JsonObject params = new JsonObject();

        String code = context.request().getParam("code");
        if (code == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<SecurityIdentity> cf = new CompletableFuture<>();

        URI absoluteUri = URI.create(context.request().absoluteURI());

        Cookie stateCookie = context.getCookie(STATE_COOKIE_NAME);
        if (stateCookie != null) {
            List<String> values = context.queryParam("state");
            // IDP must return a 'state' query parameter and the value of the state cookie must start with this parameter's value
            if (values.size() != 1) {
                LOG.debug("State parameter can not be empty or multi-valued");
                cf.completeExceptionally(new AuthenticationCompletionException());
                return cf;
            } else if (!stateCookie.getValue().startsWith(values.get(0))) {
                LOG.debug("State cookie does not match the state parameter");
                cf.completeExceptionally(new AuthenticationCompletionException());
                return cf;
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
                    if (absoluteUri.getRawQuery() != null) {
                        extraQuery += ("&" + absoluteUri.getRawQuery());
                    }

                    String localRedirectUri = buildRedirectUri(context, absoluteUri, extraPath + extraQuery);
                    LOG.debugf("Local redirect URI: %s", localRedirectUri);

                    cf.completeExceptionally(new AuthenticationRedirectException(localRedirectUri));
                    return cf;
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
            cf.completeExceptionally(new AuthenticationCompletionException());
            return cf;
        }

        // Code grant request
        // 'code': the code grant value returned from IDP
        params.put("code", code);

        // 'redirect_uri': typically it must match the 'redirect_uri' query parameter which was used during the code request.
        String redirectPath = getRedirectPath(configContext, absoluteUri);
        String redirectUriParam = buildRedirectUri(context, absoluteUri, redirectPath);
        LOG.debugf("Token request redirect_uri parameter: %s", redirectUriParam);
        params.put("redirect_uri", redirectUriParam);

        // Client secret has to be posted as a form parameter if OIDC requires the client_secret_post authentication
        Credentials creds = configContext.oidcConfig.getCredentials();
        if (creds.clientSecret.value.isPresent() && creds.clientSecret.method.isPresent()
                && Secret.Method.POST == creds.clientSecret.method.get()) {
            params.put("client_secret", creds.clientSecret.value.get());
        }

        configContext.auth.authenticate(params, userAsyncResult -> {
            if (userAsyncResult.failed()) {
                if (userAsyncResult.cause() != null) {
                    LOG.debugf("Exception during the code to token exchange: %s", userAsyncResult.cause().getMessage());
                }
                cf.completeExceptionally(new AuthenticationCompletionException(userAsyncResult.cause()));
            } else {
                AccessToken result = AccessToken.class.cast(userAsyncResult.result());

                authenticate(identityProviderManager, new IdTokenCredential(result.opaqueIdToken(), context))
                        .whenCompleteAsync((securityIdentity, throwable) -> {
                            if (throwable != null) {
                                cf.completeExceptionally(new AuthenticationCompletionException(throwable));
                            } else {
                                if (!result.idToken().containsKey("exp") || !result.idToken().containsKey("iat")) {
                                    LOG.debug("ID Token is requered to contain 'exp' and 'iat' claims");
                                    cf.completeExceptionally(new AuthenticationCompletionException(throwable));
                                }
                                processSuccessfulAuthentication(context, configContext, cf, result, securityIdentity);
                            }
                        });
            }
        });

        return cf;
    }

    private void processSuccessfulAuthentication(RoutingContext context, TenantConfigContext configContext,
            CompletableFuture<SecurityIdentity> cf,
            AccessToken result, SecurityIdentity securityIdentity) {
        removeCookie(context, configContext, SESSION_COOKIE_NAME);

        CookieImpl cookie = new CookieImpl(SESSION_COOKIE_NAME, new StringBuilder(result.opaqueIdToken())
                .append(COOKIE_DELIM)
                .append(result.opaqueAccessToken())
                .append(COOKIE_DELIM)
                .append(result.opaqueRefreshToken()).toString());
        long maxAge = result.idToken().getLong("exp") - result.idToken().getLong("iat");
        if (configContext.oidcConfig.token.expirationGrace.isPresent()) {
            maxAge += configContext.oidcConfig.token.expirationGrace.get();
        }
        LOG.debugf("Session cookie 'max-age' parameter is set to %d", maxAge);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(context.request().isSSL());
        cookie.setHttpOnly(true);
        if (configContext.oidcConfig.authentication.cookiePath.isPresent()) {
            cookie.setPath(configContext.oidcConfig.authentication.cookiePath.get());
        }
        context.response().addCookie(cookie);

        cf.complete(augmentIdentity(securityIdentity, result.opaqueAccessToken(),
                result.opaqueRefreshToken(), context));
    }

    private String getRedirectPath(TenantConfigContext configContext, URI absoluteUri) {
        Authentication auth = configContext.oidcConfig.getAuthentication();
        return auth.getRedirectPath().isPresent() ? auth.getRedirectPath().get() : absoluteUri.getRawPath();
    }

    private String generateState(RoutingContext context, TenantConfigContext configContext, URI absoluteUri,
            String redirectPath) {
        String uuid = UUID.randomUUID().toString();
        String cookieValue = uuid;

        Authentication auth = configContext.oidcConfig.getAuthentication();
        if (auth.isRestorePathAfterRedirect() && !redirectPath.equals(absoluteUri.getRawPath())) {
            cookieValue += (COOKIE_DELIM + absoluteUri.getRawPath());
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

    private String buildRedirectUri(RoutingContext context, URI absoluteUri, String path) {
        return new StringBuilder(context.request().scheme()).append("://")
                .append(absoluteUri.getAuthority())
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
}
