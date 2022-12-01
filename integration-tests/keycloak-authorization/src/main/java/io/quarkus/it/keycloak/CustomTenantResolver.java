package io.quarkus.it.keycloak;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        if (context.request().path().endsWith("tenant")) {
            return "tenant";
        }
        if (context.request().path().endsWith("webapp")) {
            return "webapp-tenant";
        }

        return null;
    }
}
