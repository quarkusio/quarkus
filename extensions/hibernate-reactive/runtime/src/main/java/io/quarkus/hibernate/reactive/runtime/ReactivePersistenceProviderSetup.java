package io.quarkus.hibernate.reactive.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.SingletonPersistenceProviderResolver;
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
        jakarta.persistence.spi.PersistenceProviderResolverHolder
                .setPersistenceProviderResolver(
                        new SingletonPersistenceProviderResolver(
                                new FastBootHibernateReactivePersistenceProvider(hibernateOrmRuntimeConfig,
                                        integrationRuntimeDescriptors)));
    }

}
