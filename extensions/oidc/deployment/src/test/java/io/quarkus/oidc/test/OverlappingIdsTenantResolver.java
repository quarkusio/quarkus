package io.quarkus.oidc.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.ext.web.RoutingContext;

/**
 * Tenant resolver which trusts the tenant id set by the already available session or state cookie
 * and resolves tenants from the request path otherwise.
 */
@ApplicationScoped
public class OverlappingIdsTenantResolver implements TenantResolver {

    private static final String TENANT_PATH_PREFIX = "/protected/tenant/";

    @Override
    public String resolve(RoutingContext context) {
        String tenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
        if (tenantId != null) {
            return tenantId;
        }
        String path = context.request().path();
        if (path.startsWith(TENANT_PATH_PREFIX)) {
            return path.substring(TENANT_PATH_PREFIX.length());
        }
        return null;
    }
}
