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

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
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

    private static final String STATE_COOKIE_NAME = "q_auth";
    private static final String SESSION_COOKIE_NAME = "q_session";
    private static final String SESSION_COOKIE_DELIM = "___";

    private static QuarkusSecurityIdentity augmentIdentity(SecurityIdentity securityIdentity,
            String accessToken,
            String refreshToken) {
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(securityIdentity.getPrincipal())
                .addCredentials(securityIdentity.getCredentials())
                .addCredential(new AccessTokenCredential(accessToken))
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
            String[] tokens = sessionCookie.getValue().split(SESSION_COOKIE_DELIM);
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
        removeSessionCookie(context);
        ChallengeData challenge;

        JsonObject params = new JsonObject();

        List<Object> scopes = new ArrayList<>();

        scopes.add("openid");
        scopes.addAll(config.authentication.scopes);

        params.put("scopes", new JsonArray(scopes));
        params.put("redirect_uri", buildRedirectUri(context));
        params.put("state", generateState(context));

        challenge = new ChallengeData(HttpResponseStatus.FOUND.code(), HttpHeaders.LOCATION, auth.authorizeURL(params));

        return CompletableFuture.completedFuture(challenge);
    }

    private CompletionStage<SecurityIdentity> performCodeFlow(IdentityProviderManager identityProviderManager,
            RoutingContext context) {
        CompletableFuture<SecurityIdentity> cf = new CompletableFuture<>();
        JsonObject params = new JsonObject();

        params.put("code", context.request().getParam("code"));
        params.put("redirect_uri", buildRedirectUri(context));

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

    private void processSuccessfulAuthentication(RoutingContext context, CompletableFuture<SecurityIdentity> cf,
            AccessToken result, SecurityIdentity securityIdentity) {
        removeSessionCookie(context);
        CookieImpl cookie = new CookieImpl(SESSION_COOKIE_NAME, new StringBuilder(result.opaqueIdToken())
                .append(SESSION_COOKIE_DELIM)
                .append(result.opaqueAccessToken())
                .append(SESSION_COOKIE_DELIM)
                .append(result.opaqueRefreshToken()).toString());

        cookie.setMaxAge(result.idToken().getInteger("exp"));
        cookie.setSecure(context.request().isSSL());
        cookie.setHttpOnly(true);

        context.response().addCookie(cookie);
        cf.complete(augmentIdentity(securityIdentity, result.opaqueAccessToken(),
                result.opaqueRefreshToken()));
    }

    private String generateState(RoutingContext context) {
        CookieImpl cookie = new CookieImpl(STATE_COOKIE_NAME, UUID.randomUUID().toString());

        cookie.setHttpOnly(true);
        cookie.setSecure(context.request().isSSL());
        cookie.setMaxAge(-1);

        context.response().addCookie(cookie);

        return cookie.getValue();
    }

    private String buildRedirectUri(RoutingContext context) {
        URI absoluteUri = URI.create(context.request().absoluteURI());
        StringBuilder builder = new StringBuilder(context.request().scheme()).append("://")
                .append(absoluteUri.getAuthority())
                .append(absoluteUri.getPath());

        return builder.toString();
    }

    private void removeSessionCookie(RoutingContext context) {
        context.response().removeCookie(SESSION_COOKIE_NAME, true);
    }
}
