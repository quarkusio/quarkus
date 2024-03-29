package io.quarkus.it.hibernate.multitenancy.fruit;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@PersistenceUnitExtension
@RequestScoped
public class CustomTenantResolver implements TenantResolver {

    private static final Logger LOG = Logger.getLogger(CustomTenantResolver.class);

    @Inject
    RoutingContext context;

    @Override
    public String getDefaultTenantId() {
        return "base";
    }

    @Override
    public String resolveTenantId() {
        String path = context.request().path();
        final String tenantId;
        if (path.startsWith("/mycompany")) {
            tenantId = "mycompany";
        } else if (path.startsWith("/global")) {
            tenantId = "global";
        } else {
            tenantId = getDefaultTenantId();
        }
        LOG.debugv("TenantId = {0}", tenantId);
        return tenantId;
    }

    @Override
    public boolean isRoot(String tenantId) {
        return "global".equals(tenantId);
    }

}
