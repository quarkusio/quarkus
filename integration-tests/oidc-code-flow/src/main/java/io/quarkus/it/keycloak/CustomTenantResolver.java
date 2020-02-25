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
        return path.contains("callback-") ? "tenant-1" : path.contains("/web-app2") ? "tenant-2" : null;
    }
}
