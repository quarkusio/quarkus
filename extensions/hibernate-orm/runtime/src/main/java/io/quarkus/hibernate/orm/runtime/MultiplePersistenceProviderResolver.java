package io.quarkus.hibernate.orm.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceProviderResolver;

public final class MultiplePersistenceProviderResolver implements PersistenceProviderResolver {

    private final List<PersistenceProvider> persistenceProviders = new ArrayList<>();

    public MultiplePersistenceProviderResolver(PersistenceProvider... persistenceProviders) {
        this.persistenceProviders.addAll(List.of(persistenceProviders));
    }

    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        return Collections.unmodifiableList(persistenceProviders);
    }

    public void addPersistenceProvider(PersistenceProvider persistenceProvider) {
        persistenceProviders.add(persistenceProvider);
    }

    @Override
    public void clearCachedProviders() {
        // done!
    }

}
