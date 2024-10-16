package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcUtils.extractBearerToken;

import java.util.function.Function;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class BearerAuthenticationMechanism extends AbstractOidcAuthenticationMechanism {
    private static final Logger LOG = Logger.getLogger(BearerAuthenticationMechanism.class);

    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager, OidcTenantConfig oidcTenantConfig) {
        LOG.debug("Starting a bearer access token authentication");
        String token = extractBearerToken(context, oidcTenantConfig);
        // if a bearer token is provided try to authenticate
        if (token != null) {
            return authenticate(identityProviderManager, context, new AccessTokenCredential(token));
        }
        LOG.debug("Bearer access token is not available");
        return Uni.createFrom().nullItem();
    }

    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        Uni<TenantConfigContext> tenantContext = resolver.resolveContext(context);
        return tenantContext.onItem().transformToUni(new Function<TenantConfigContext, Uni<? extends ChallengeData>>() {
            @Override
            public Uni<ChallengeData> apply(TenantConfigContext tenantContext) {
                return Uni.createFrom().item(new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(),
                        HttpHeaderNames.WWW_AUTHENTICATE, tenantContext.oidcConfig().token.authorizationScheme));
            }
        });
    }
}
