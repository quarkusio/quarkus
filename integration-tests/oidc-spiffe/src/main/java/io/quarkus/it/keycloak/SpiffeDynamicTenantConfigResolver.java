package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

// point here is that we are able to create a new tenant dynamically
// and this tenant doesn't have cached JWT-SVID, so we are able to test recovery
// after the client failed to retrieve token
@ApplicationScoped
class SpiffeDynamicTenantConfigResolver implements TenantConfigResolver {

    @Inject
    OidcConfig oidcConfig;

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        String tenant = context.request().getParam("tenant");
        if (tenant != null) {
            OidcTenantConfig config = OidcTenantConfig
                    .builder(oidcConfig.namedTenants().get(OidcConfig.DEFAULT_TENANT_KEY))
                    .tenantId("spiffe-dynamic-" + tenant)
                    .build();
            return Uni.createFrom().item(config);
        }
        return Uni.createFrom().nullItem();
    }
}
