package io.quarkus.oidc.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {
    @Override
    public String resolve(RoutingContext context) {
        if (context.request().path().endsWith("/tenant-resolver")) {
            return "tenant-resolver";
        }
        context.put(OidcUtils.TENANT_ID_ATTRIBUTE, OidcUtils.DEFAULT_TENANT_ID);
        return null;
    }
}
