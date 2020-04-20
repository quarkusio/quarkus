package io.quarkus.it.keycloak;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        List<String> tenantId = context.queryParam("tenantId");
        if (!tenantId.isEmpty()) {
            return tenantId.get(0).isEmpty() ? null : tenantId.get(0);
        }

        String path = context.request().path();

        if (path.contains("tenant-logout")) {
            return "tenant-logout";
        }

        return path.contains("callback-after-redirect") || path.contains("callback-before-redirect") ? "tenant-1"
                : path.contains("callback-jwt-after-redirect") || path.contains("callback-jwt-before-redirect") ? "tenant-jwt"
                        : path.contains("callback-jwt-not-used-after-redirect")
                                || path.contains("callback-jwt-not-used-before-redirect")
                                        ? "tenant-jwt-not-used"
                                        : path.contains("/web-app2") ? "tenant-2" : null;
    }
}
