package io.quarkus.hibernate.reactive.runtime;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.SingletonPersistenceProviderResolver;

public final class ReactivePersistenceProviderSetup {

    private ReactivePersistenceProviderSetup() {
        // not to be constructed
    }

    public static void registerStaticInitPersistenceProvider() {
        javax.persistence.spi.PersistenceProviderResolverHolder
                .setPersistenceProviderResolver(new StaticInitHibernateReactivePersistenceProviderResolver());
    }

    public static void registerRuntimePersistenceProvider(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig) {
        javax.persistence.spi.PersistenceProviderResolverHolder
                .setPersistenceProviderResolver(
                        new SingletonPersistenceProviderResolver(
                                new FastBootHibernateReactivePersistenceProvider(hibernateOrmRuntimeConfig)));
    }

}
