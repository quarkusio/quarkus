package io.quarkus.hibernate.reactive.runtime;

import java.util.List;
import java.util.Map;

import jakarta.persistence.spi.PersistenceProviderResolver;
import jakarta.persistence.spi.PersistenceProviderResolverHolder;

import io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.MultiplePersistenceProviderResolver;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;

public final class ReactivePersistenceProviderSetup {

    private ReactivePersistenceProviderSetup() {
        // not to be constructed
    }

    public static void registerStaticInitPersistenceProvider() {
        jakarta.persistence.spi.PersistenceProviderResolverHolder
                .setPersistenceProviderResolver(new StaticInitHibernateReactivePersistenceProviderResolver());
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
                .addPersistenceProvider(new FastBootHibernateReactivePersistenceProvider(hibernateOrmRuntimeConfig,
                        integrationRuntimeDescriptors));
        ((MultiplePersistenceProviderResolver) persistenceProviderResolver)
                .addPersistenceProvider(new FastBootHibernatePersistenceProvider(hibernateOrmRuntimeConfig,
                        integrationRuntimeDescriptors));

    }

}
