package io.quarkus.oidc.runtime;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class BearerAuthenticationMechanism extends AbstractOidcAuthenticationMechanism {

    private static final String BEARER = "Bearer";
    protected static final ChallengeData UNAUTHORIZED_CHALLENGE = new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(),
            null, null);
    protected static final ChallengeData FORBIDDEN_CHALLENGE = new ChallengeData(HttpResponseStatus.FORBIDDEN.code(), null,
            null);

    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager,
            DefaultTenantConfigResolver resolver) {
        String token = extractBearerToken(context);

        // if a bearer token is provided try to authenticate
        if (token != null) {
            return authenticate(identityProviderManager, new AccessTokenCredential(token, context));
        }
        return Uni.createFrom().nullItem();
    }

    public Uni<ChallengeData> getChallenge(RoutingContext context, DefaultTenantConfigResolver resolver) {
        String bearerToken = extractBearerToken(context);

        if (bearerToken == null) {
            return Uni.createFrom().item(UNAUTHORIZED_CHALLENGE);
        }

        return Uni.createFrom().item(FORBIDDEN_CHALLENGE);
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

        return authorization.substring(idx + 1);
    }
}
