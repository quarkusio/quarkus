package io.quarkus.it.hibernate.multitenancy;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;

@Singleton
public class Producers {

    @Inject
    ConnectionConfig config;

    @Produces
    @Unremovable
    @ApplicationScoped
    @PersistenceUnitExtension
    CustomTenantConnectionResolver defaultConnectionResolver() {
        return new CustomTenantConnectionResolver(config, "default");
    }

    void disposeDefaultConnectionResolver(@Disposes @PersistenceUnitExtension CustomTenantConnectionResolver resolver) {
        resolver.close();
    }

    @Produces
    @Unremovable
    @ApplicationScoped
    @PersistenceUnitExtension("inventory")
    CustomTenantConnectionResolver inventoryConnectionResolver() {
        return new CustomTenantConnectionResolver(config, "inventory");
    }

    void disposeInventoryConnectionResolver(
            @Disposes @PersistenceUnitExtension("inventory") CustomTenantConnectionResolver resolver) {
        resolver.close();
    }

}
