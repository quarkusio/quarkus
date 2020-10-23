package io.quarkus.hibernate.reactive.runtime;

import java.util.Collections;
import java.util.List;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;

final class FastBootHibernateReactivePersistenceProviderResolver implements PersistenceProviderResolver {

    private final List<PersistenceProvider> persistenceProviders;

    public FastBootHibernateReactivePersistenceProviderResolver(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig) {
        persistenceProviders = Collections
                .singletonList(new FastBootHibernateReactivePersistenceProvider(hibernateOrmRuntimeConfig));
    }

    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        return persistenceProviders;
    }

    @Override
    public void clearCachedProviders() {
        // done!
    }
}
