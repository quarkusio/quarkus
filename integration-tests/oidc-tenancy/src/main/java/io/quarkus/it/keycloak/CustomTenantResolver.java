package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        // Make sure this resolver is called only once during a given request
        if (context.get("static_config_resolved") != null) {
            throw new RuntimeException();
        }
        context.put("static_config_resolved", "true");

        if (context.request().path().endsWith("/tenant-public-key")) {
            return "tenant-public-key";
        }
        String tenantId = context.request().path().split("/")[2];
        if ("tenant-hybrid".equals(tenantId)) {
            return context.request().getHeader("Authorization") != null ? "tenant-hybrid-service" : "tenant-hybrid-webapp";
        }

        if ("tenant-web-app".equals(tenantId)
                && context.getCookie("q_session_tenant-web-app") != null) {
            context.put("reauthenticated", "true");
            return context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
        }

        return tenantId;

    }
}
