package io.quarkus.it.keycloak;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        String path = context.normalizedPath();
        if (path.endsWith("code-flow")) {
            return "code-flow";
        }
        if (path.endsWith("code-flow-user-info")) {
            return "code-flow-user-info-only";
        }
        if (path.endsWith("bearer")) {
            return "bearer";
        }
        if (path.endsWith("bearer-no-introspection")) {
            return "bearer-no-introspection";
        }
        if (path.endsWith("bearer-wrong-role-path")) {
            return "bearer-wrong-role-path";
        }
        return null;
    }
}