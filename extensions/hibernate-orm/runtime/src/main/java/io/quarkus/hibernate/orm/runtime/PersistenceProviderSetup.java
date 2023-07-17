package io.quarkus.hibernate.orm.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;

public final class PersistenceProviderSetup {

    private PersistenceProviderSetup() {
        // not to be constructed
    }

    public static void registerStaticInitPersistenceProvider() {
        jakarta.persistence.spi.PersistenceProviderResolverHolder
                .setPersistenceProviderResolver(new StaticInitHibernatePersistenceProviderResolver());
    }

    public static void registerRuntimePersistenceProvider(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig,
            Map<String, List<HibernateOrmIntegrationRuntimeDescriptor>> integrationRuntimeDescriptors) {
        jakarta.persistence.spi.PersistenceProviderResolverHolder.setPersistenceProviderResolver(
                new SingletonPersistenceProviderResolver(
                        new FastBootHibernatePersistenceProvider(hibernateOrmRuntimeConfig, integrationRuntimeDescriptors)));
    }
}
