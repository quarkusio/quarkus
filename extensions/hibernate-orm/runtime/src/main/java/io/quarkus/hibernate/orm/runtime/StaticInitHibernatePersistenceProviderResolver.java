package io.quarkus.hibernate.orm.runtime;

import java.util.List;

import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceProviderResolver;

/**
 * During the static init phase, we don't access the PersistenceProviderResolver.
 */
final class StaticInitHibernatePersistenceProviderResolver implements PersistenceProviderResolver {

    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        throw new IllegalStateException("Persistence providers are not available during the static init phase.");
    }

    @Override
    public void clearCachedProviders() {
        throw new IllegalStateException("Persistence providers are not available during the static init phase.");
    }

}
