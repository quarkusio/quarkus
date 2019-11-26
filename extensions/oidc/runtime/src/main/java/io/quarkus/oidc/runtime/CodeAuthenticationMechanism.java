package io.quarkus.oidc.runtime;

import java.net.URI;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.AuthenticationRedirectException;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.CookieImpl;

@ApplicationScoped
public class CodeAuthenticationMechanism extends AbstractOidcAuthenticationMechanism {

    private static final Logger LOG = Logger.getLogger(CodeAuthenticationMechanism.class);

    private static final String STATE_COOKIE_NAME = "q_auth";
    private static final String SESSION_COOKIE_NAME = "q_session";
    private static final String COOKIE_DELIM = "___";

    private static QuarkusSecurityIdentity augmentIdentity(SecurityIdentity securityIdentity,
            String accessToken,
            String refreshToken) {
        final RefreshToken refreshTokenCredential = new RefreshToken(refreshToken);
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(securityIdentity.getPrincipal())
                .addCredentials(securityIdentity.getCredentials())
                .addCredential(new AccessTokenCredential(accessToken, refreshTokenCredential))
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

    @Override
    public CompletionStage<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        Cookie sessionCookie = context.request().getCookie(SESSION_COOKIE_NAME);

        // if session already established, try to re-authenticate
        if (sessionCookie != null) {
            String[] tokens = sessionCookie.getValue().split(COOKIE_DELIM);
            return authenticate(identityProviderManager, new IdTokenCredential(tokens[0]))
                    .thenCompose(new Function<SecurityIdentity, CompletionStage<SecurityIdentity>>() {
                        @Override
                        public CompletionStage<SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                            return CompletableFuture.completedFuture(augmentIdentity(securityIdentity, tokens[1], tokens[2]));
                        }
                    });
        }

        // start a new session by starting the code flow dance
        return performCodeFlow(identityProviderManager, context);
    }

    @Override
    public CompletionStage<ChallengeData> getChallenge(RoutingContext context) {
        removeCookie(context, SESSION_COOKIE_NAME);
        ChallengeData challenge;
        JsonObject params = new JsonObject();

        List<Object> scopes = new ArrayList<>();

        scopes.add("openid");
        config.authentication.scopes.ifPresent(scopes::addAll);

        params.put("scopes", new JsonArray(scopes));

        URI absoluteUri = URI.create(context.request().absoluteURI());
        String dynamicPath = getDynamicPath(context, absoluteUri);
        params.put("redirect_uri", buildCodeRedirectUri(context, absoluteUri, dynamicPath));

        params.put("state", generateState(context, dynamicPath));

        challenge = new ChallengeData(HttpResponseStatus.FOUND.code(), HttpHeaders.LOCATION, auth.authorizeURL(params));

        return CompletableFuture.completedFuture(challenge);
    }

    private CompletionStage<SecurityIdentity> performCodeFlow(IdentityProviderManager identityProviderManager,
            RoutingContext context) {
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
            if (values.size() != 1 || !stateCookie.getValue().startsWith(values.get(0))) {
                cf.completeExceptionally(new AuthenticationFailedException());
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

                    String localRedirectUri = buildLocalRedirectUri(context, absoluteUri, extraPath + extraQuery);
                    LOG.debug("Local redirectUri: " + localRedirectUri);

                    cf.completeExceptionally(new AuthenticationRedirectException(localRedirectUri));
                    return cf;
                }
                // The redirect path matches the original request path, the state cookie is no longer needed
                removeCookie(context, STATE_COOKIE_NAME);
            } else {
                // Local redirect restoring the original request path, the state cookie is no longer needed
                removeCookie(context, STATE_COOKIE_NAME);
            }
        } else {
            // State cookie must be available to minimize the risk of CSRF
            cf.completeExceptionally(new AuthenticationFailedException());
            return cf;
        }

        params.put("code", code);
        params.put("redirect_uri", buildCodeRedirectUri(context, absoluteUri, getDynamicPath(context, absoluteUri)));

        auth.authenticate(params, userAsyncResult -> {
            if (userAsyncResult.failed()) {
                cf.completeExceptionally(new AuthenticationFailedException());
            } else {
                AccessToken result = AccessToken.class.cast(userAsyncResult.result());

                authenticate(identityProviderManager, new IdTokenCredential(result.opaqueIdToken()))
                        .whenCompleteAsync((securityIdentity, throwable) -> {
                            if (throwable != null) {
                                cf.completeExceptionally(throwable);
                            } else {
                                processSuccessfulAuthentication(context, cf, result, securityIdentity);
                            }
                        });
            }
        });

        return cf;
    }

    private void processSuccessfulAuthentication(RoutingContext context,
            CompletableFuture<SecurityIdentity> cf,
            AccessToken result, SecurityIdentity securityIdentity) {
        removeCookie(context, SESSION_COOKIE_NAME);
        CookieImpl cookie = new CookieImpl(SESSION_COOKIE_NAME, new StringBuilder(result.opaqueIdToken())
                .append(COOKIE_DELIM)
                .append(result.opaqueAccessToken())
                .append(COOKIE_DELIM)
                .append(result.opaqueRefreshToken()).toString());

        cookie.setMaxAge(result.idToken().getInteger("exp"));
        cookie.setSecure(context.request().isSSL());
        cookie.setHttpOnly(true);
        context.response().addCookie(cookie);

        cf.complete(augmentIdentity(securityIdentity, result.opaqueAccessToken(),
                result.opaqueRefreshToken()));
    }

    private String getDynamicPath(RoutingContext context, URI absoluteUri) {
        if (config.authentication.redirectPath.isPresent()) {
            String redirectPath = config.authentication.redirectPath.get();
            String requestPath = absoluteUri.getRawPath();
            if (requestPath.startsWith(redirectPath) && requestPath.length() > redirectPath.length()) {
                return requestPath.substring(redirectPath.length());
            }
        }
        return null;
    }

    private String generateState(RoutingContext context, String dynamicPath) {
        String uuid = UUID.randomUUID().toString();
        String cookieValue = uuid;
        if (dynamicPath != null) {
            cookieValue += (COOKIE_DELIM + dynamicPath);
        }

        CookieImpl cookie = new CookieImpl(STATE_COOKIE_NAME, cookieValue);

        cookie.setHttpOnly(true);
        cookie.setSecure(context.request().isSSL());
        // max-age is 30 minutes
        cookie.setMaxAge(60 * 30);

        context.response().addCookie(cookie);
        return uuid;
    }

    private String buildCodeRedirectUri(RoutingContext context, URI absoluteUri, String dynamicPath) {
        StringBuilder builder = new StringBuilder(context.request().scheme()).append("://")
                .append(absoluteUri.getAuthority());

        String path = dynamicPath != null
                ? config.authentication.redirectPath.get()
                : absoluteUri.getRawPath();

        return builder.append(path).toString();
    }

    private String buildLocalRedirectUri(RoutingContext context, URI absoluteUri, String extraPath) {
        return new StringBuilder(context.request().scheme()).append("://")
                .append(absoluteUri.getAuthority())
                .append(absoluteUri.getRawPath())
                .append(extraPath)
                .toString();
    }

    private Cookie removeCookie(RoutingContext context, String cookieName) {
        return context.response().removeCookie(cookieName, true);
    }
}
