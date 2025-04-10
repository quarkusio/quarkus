package io.quarkus.hibernate.orm.runtime;

import java.util.List;
import java.util.Map;

import jakarta.persistence.spi.PersistenceProviderResolver;
import jakarta.persistence.spi.PersistenceProviderResolverHolder;

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

        PersistenceProviderResolver persistenceProviderResolver = PersistenceProviderResolverHolder
                .getPersistenceProviderResolver();
        if (persistenceProviderResolver == null ||
                (persistenceProviderResolver != null
                        && !(persistenceProviderResolver instanceof MultiplePersistenceProviderResolver))) {
            persistenceProviderResolver = new MultiplePersistenceProviderResolver();
            PersistenceProviderResolverHolder.setPersistenceProviderResolver(persistenceProviderResolver);
        }

        ((MultiplePersistenceProviderResolver) persistenceProviderResolver)
                .addPersistenceProvider(new FastBootHibernatePersistenceProvider(hibernateOrmRuntimeConfig,
                        integrationRuntimeDescriptors));

    }
}
