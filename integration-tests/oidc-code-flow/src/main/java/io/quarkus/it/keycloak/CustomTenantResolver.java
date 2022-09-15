package io.quarkus.it.keycloak;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.runtime.OidcUtils;
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

        if (path.contains("tenant-query")) {
            return "tenant-query";
        }

        if (path.contains("tenant-listener")) {
            return "tenant-listener";
        }

        if (path.contains("tenant-idtoken-only")) {
            return "tenant-idtoken-only";
        }

        if (path.contains("tenant-split-tokens")) {
            return "tenant-split-tokens";
        }

        if (path.contains("tenant-id-refresh-token")) {
            return "tenant-id-refresh-token";
        }

        if (path.contains("tenant-split-id-refresh-token")) {
            return "tenant-split-id-refresh-token";
        }

        if (path.contains("tenant-autorefresh")) {
            return "tenant-autorefresh";
        }

        if (path.contains("tenant-https")) {
            if (context.getCookie("q_session_tenant-https_test") != null) {
                context.put("reauthenticated", "true");
                return context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
            } else {
                return "tenant-https";
            }
        }

        if (path.contains("tenant-xhr")) {
            return "tenant-xhr";
        }

        if (path.contains("tenant-javascript")) {
            return "tenant-javascript";
        }

        if (path.contains("tenant-cookie-path-header")) {
            return "tenant-cookie-path-header";
        }

        if (path.contains("callback-after-redirect") || path.contains("callback-before-redirect")) {
            return "tenant-1";
        }

        if (path.contains("callback-jwt-after-redirect") || path.contains("callback-jwt-before-redirect")) {
            return "tenant-jwt";
        }

        if (path.contains("callback-jwt-not-used-after-redirect") || path.contains("callback-jwt-not-used-before-redirect")) {
            return "tenant-jwt-not-used";
        }

        if (path.contains("/web-app2")) {
            return "tenant-2";
        }

        return OidcUtils.DEFAULT_TENANT_ID;
    }
}
