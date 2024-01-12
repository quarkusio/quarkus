package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        String path = context.normalizedPath();
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
