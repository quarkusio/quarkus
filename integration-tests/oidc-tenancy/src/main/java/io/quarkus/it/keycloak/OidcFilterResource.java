package io.quarkus.it.keycloak;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/oidc-filter")
public class OidcFilterResource {

    @Inject
    CustomBearerTokenAuthRequestFilter bearerTokenAuthRequestFilter;

    @Any
    @Inject
    CustomTenantFeatureAuthResponseFilter tenantFeatureAuthResponseFilter;

    @GET
    @Path("/request/custom-bearer-token-auth")
    public String getAndResetRequestFilter() {
        return bearerTokenAuthRequestFilter.getAndReset();
    }

    @GET
    @Path("/response/custom-tenant-feature-auth")
    public String getAndResetResponseFilter() {
        return tenantFeatureAuthResponseFilter.getAndReset();
    }

}
