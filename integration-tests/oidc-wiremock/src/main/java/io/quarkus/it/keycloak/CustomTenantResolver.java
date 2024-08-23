package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        String path = context.normalizedPath();
        // `/hr-classic-perm-check` and '/hr-classic-and-jaxrs-perm-check'
        // require policy checks which force an authentication before @Tenant is resolved
        if (path.contains("/hr") && !path.contains("/hr-classic-perm-check")
                && !path.contains("/hr-classic-and-jaxrs-perm-check")) {
            throw new RuntimeException("@Tenant annotation only must be used to set "
                    + "a tenant id on the '" + path + "' request path");
        }
        if (context.get(OidcUtils.TENANT_ID_ATTRIBUTE) != null) {
            if (context.get(OidcUtils.TENANT_ID_SET_BY_SESSION_COOKIE) == null
                    && context.get(OidcUtils.TENANT_ID_SET_BY_STATE_COOKIE) == null) {
                throw new RuntimeException("Tenant id must have been set by either the session or state cookie");
            }
            // Expect an already resolved tenant context be used
            return null;
        }
        if (path.contains("recovered-no-discovery")) {
            return "no-discovery";
        }
        if (path.endsWith("code-flow") || path.endsWith("code-flow/logout")) {
            return "code-flow";
        }
        if (path.endsWith("code-flow-form-post") || path.endsWith("code-flow-form-post/front-channel-logout")) {
            return "code-flow-form-post";
        }

        return null;
    }
}
