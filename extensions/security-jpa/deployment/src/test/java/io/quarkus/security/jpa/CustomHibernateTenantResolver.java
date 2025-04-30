package io.quarkus.security.jpa;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@PersistenceUnitExtension
@RequestScoped
public class CustomHibernateTenantResolver implements TenantResolver {

    static volatile boolean useRoutingContext = false;

    @Inject
    RoutingContext routingContext;

    @Override
    public String getDefaultTenantId() {
        return "one";
    }

    @Override
    public String resolveTenantId() {
        if (useRoutingContext) {
            var tenant = routingContext.queryParam("tenant");
            if (!tenant.isEmpty()) {
                return tenant.get(0);
            }
        }
        return "two";
    }

}
