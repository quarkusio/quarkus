package io.quarkus.it.hibernate.multitenancy.inventory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@PersistenceUnitExtension("inventory")
@RequestScoped
public class InventoryTenantResolver implements TenantResolver {

    private static final Logger LOG = Logger.getLogger(InventoryTenantResolver.class);

    @Inject
    RoutingContext context;

    @Override
    public String getDefaultTenantId() {
        return "inventory";
    }

    @Override
    public String resolveTenantId() {
        String path = context.request().path();
        final String tenantId;
        if (path.startsWith("/mycompany")) {
            tenantId = "inventorymycompany";
        } else {
            tenantId = getDefaultTenantId();
        }
        LOG.debugv("TenantId = {0}", tenantId);
        return tenantId;
    }

}
