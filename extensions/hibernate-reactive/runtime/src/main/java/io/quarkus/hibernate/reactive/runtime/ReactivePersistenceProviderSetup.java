package io.quarkus.hibernate.reactive.runtime;

public final class ReactivePersistenceProviderSetup {

    private ReactivePersistenceProviderSetup() {
        // not to be constructed
    }

    public static void registerPersistenceProvider() {
        javax.persistence.spi.PersistenceProviderResolverHolder
                .setPersistenceProviderResolver(new FastBootHibernateReactivePersistenceProviderResolver());
    }

}
