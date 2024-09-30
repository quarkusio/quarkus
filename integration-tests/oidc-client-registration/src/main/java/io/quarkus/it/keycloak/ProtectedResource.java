package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.OidcSession;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.security.Authenticated;

@Path("/protected")
@Authenticated
public class ProtectedResource {

    @Inject
    @IdToken
    JsonWebToken principal;

    @Inject
    OidcSession session;

    @Inject
    TenantConfigBean tenantConfigBean;

    @GET
    public String principalName() {
        return session.getTenantId() + ":" + getClientName() + ":" + principal.getName();
    }

    @GET
    @Path("/tenant")
    public String tenantPrincipalName() {
        return session.getTenantId() + ":" + getClientName() + ":" + principal.getName();
    }

    @GET
    @Path("/dynamic")
    public String dynamicPrincipalName() {
        return session.getTenantId() + ":" + getClientName() + ":" + principal.getName();
    }

    @GET
    @Path("/dynamic-tenant")
    public String dynamicTenantPrincipalName() {
        return session.getTenantId() + ":" + getClientName() + ":" + principal.getName();
    }

    @GET
    @Path("/multi1")
    public String principalNameMulti1() {
        return session.getTenantId() + ":" + getClientName() + ":" + principal.getName();
    }

    @GET
    @Path("/multi2")
    public String principalNameMulti2() {
        return session.getTenantId() + ":" + getClientName() + ":" + principal.getName();
    }

    private String getClientName() {
        OidcTenantConfig oidcConfig = tenantConfigBean.getDynamicTenant(session.getTenantId())
                .getOidcTenantConfig();
        return oidcConfig.getClientName().get();
    }
}
