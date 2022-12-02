package io.quarkus.hibernate.orm.runtime;

import java.util.Collections;
import java.util.List;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;

public final class SingletonPersistenceProviderResolver implements PersistenceProviderResolver {

    private final List<PersistenceProvider> persistenceProviders;

    public SingletonPersistenceProviderResolver(PersistenceProvider singleton) {
        persistenceProviders = Collections.singletonList(singleton);
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
