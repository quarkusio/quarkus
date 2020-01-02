package io.quarkus.oidc.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class BearerAuthenticationMechanism extends AbstractOidcAuthenticationMechanism {

    @Override
    public CompletionStage<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        String token = extractBearerToken(context);

        // if a bearer token is provided try to authenticate
        if (token != null) {
            return authenticate(identityProviderManager, new AccessTokenCredential(token, context));
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<ChallengeData> getChallenge(RoutingContext context) {
        String bearerToken = extractBearerToken(context);

        if (bearerToken == null) {
            return CompletableFuture.completedFuture(new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(), null, null));
        }

        return CompletableFuture.completedFuture(new ChallengeData(HttpResponseStatus.FORBIDDEN.code(), null, null));
    }

    private String extractBearerToken(RoutingContext context) {
        final HttpServerRequest request = context.request();
        final String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

        if (authorization == null) {
            return null;
        }

        int idx = authorization.indexOf(' ');

        if (idx <= 0 || !BEARER.equalsIgnoreCase(authorization.substring(0, idx))) {
            return null;
        }

        String token = authorization.substring(idx + 1);
        return token;
    }
}
