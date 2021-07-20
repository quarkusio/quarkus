package io.quarkus.it.hibernate.multitenancy;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.PersistenceUnit;

@Singleton
public class Producers {

    @Inject
    ConnectionConfig config;

    @Produces
    @Unremovable
    @ApplicationScoped
    @Default
    CustomTenantConnectionResolver defaultConnectionResolver() {
        return new CustomTenantConnectionResolver(config, "default");
    }

    void disposeDefaultConnectionResolver(@Disposes @Default CustomTenantConnectionResolver resolver) {
        resolver.close();
    }

    @Produces
    @Unremovable
    @ApplicationScoped
    @PersistenceUnit("inventory")
    CustomTenantConnectionResolver inventoryConnectionResolver() {
        return new CustomTenantConnectionResolver(config, "inventory");
    }

    void disposeInventoryConnectionResolver(@Disposes @PersistenceUnit("inventory") CustomTenantConnectionResolver resolver) {
        resolver.close();
    }

}
