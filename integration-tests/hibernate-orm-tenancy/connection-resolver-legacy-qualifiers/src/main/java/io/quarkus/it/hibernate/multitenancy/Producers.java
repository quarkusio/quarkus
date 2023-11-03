package io.quarkus.it.hibernate.multitenancy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
