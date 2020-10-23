package io.quarkus.hibernate.orm.runtime;

public final class PersistenceProviderSetup {

    private PersistenceProviderSetup() {
        // not to be constructed
    }

    public static void registerStaticInitPersistenceProvider() {
        javax.persistence.spi.PersistenceProviderResolverHolder
                .setPersistenceProviderResolver(new StaticInitHibernatePersistenceProviderResolver());
    }

    public static void registerRuntimePersistenceProvider(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig) {
        javax.persistence.spi.PersistenceProviderResolverHolder
                .setPersistenceProviderResolver(new FastBootHibernatePersistenceProviderResolver(hibernateOrmRuntimeConfig));
    }
}
