package io.quarkus.hibernate.orm.runtime;

import java.util.Collections;
import java.util.List;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;

final class FastBootHibernatePersistenceProviderResolver implements PersistenceProviderResolver {

    private final List<PersistenceProvider> persistenceProviders;

    public FastBootHibernatePersistenceProviderResolver(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig) {
        persistenceProviders = Collections.singletonList(new FastBootHibernatePersistenceProvider(hibernateOrmRuntimeConfig));
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
