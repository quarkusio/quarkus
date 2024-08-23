package io.quarkus.it.keycloak;

import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    private static final Predicate<String> USE_DEFAULT_STATIC_RESOLVER = path -> Stream
            .of("/api/tenant-echo", "/api/tenant-paths/")
            .anyMatch(path::contains);

    @Override
    public String resolve(RoutingContext context) {
        if (USE_DEFAULT_STATIC_RESOLVER.test(context.request().path())) {
            return null;
        }
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
