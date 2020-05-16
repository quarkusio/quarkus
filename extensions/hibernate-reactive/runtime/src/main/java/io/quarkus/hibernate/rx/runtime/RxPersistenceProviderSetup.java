package io.quarkus.hibernate.rx.runtime;

public final class RxPersistenceProviderSetup {

    private RxPersistenceProviderSetup() {
        // not to be constructed
    }

    public static void registerPersistenceProvider() {
        javax.persistence.spi.PersistenceProviderResolverHolder
                .setPersistenceProviderResolver(new FastBootHibernateRxPersistenceProviderResolver());
    }

}
