package io.quarkus.oidc.runtime;

import java.util.function.Function;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class BearerAuthenticationMechanism extends AbstractOidcAuthenticationMechanism {

    protected static final ChallengeData UNAUTHORIZED_CHALLENGE = new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(),
            HttpHeaderNames.WWW_AUTHENTICATE, OidcConstants.BEARER_SCHEME);

    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        return resolver.resolveConfig(context).chain(new Function<OidcTenantConfig, Uni<? extends SecurityIdentity>>() {
            @Override
            public Uni<? extends SecurityIdentity> apply(OidcTenantConfig oidcTenantConfig) {
                String token = extractBearerToken(context, oidcTenantConfig);
                // if a bearer token is provided try to authenticate
                if (token != null) {
                    return authenticate(identityProviderManager, context, new AccessTokenCredential(token, context));
                }
                return Uni.createFrom().nullItem();
            }
        });
    }

    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(UNAUTHORIZED_CHALLENGE);
    }

    private String extractBearerToken(RoutingContext context, OidcTenantConfig oidcConfig) {
        final HttpServerRequest request = context.request();
        String header = oidcConfig.token.header.isPresent() ? oidcConfig.token.header.get()
                : HttpHeaders.AUTHORIZATION.toString();
        final String headerValue = request.headers().get(header);

        if (headerValue == null) {
            return null;
        }

        int idx = headerValue.indexOf(' ');
        final String scheme = idx > 0 ? headerValue.substring(0, idx) : null;

        if (scheme == null && !header.equalsIgnoreCase(HttpHeaders.AUTHORIZATION.toString())) {
            return headerValue;
        }

        if (!OidcConstants.BEARER_SCHEME.equalsIgnoreCase(scheme)) {
            return null;
        }

        return headerValue.substring(idx + 1);
    }
}
