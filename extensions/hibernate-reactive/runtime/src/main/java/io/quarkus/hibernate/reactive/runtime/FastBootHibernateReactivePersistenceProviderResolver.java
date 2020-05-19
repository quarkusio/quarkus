package io.quarkus.hibernate.reactive.runtime;

import java.util.Collections;
import java.util.List;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;

final class FastBootHibernateReactivePersistenceProviderResolver implements PersistenceProviderResolver {

    private static final List<PersistenceProvider> HARDCODED_PROVIDER_LIST = Collections
            .singletonList(new FastBootHibernateReactivePersistenceProvider());

    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        return HARDCODED_PROVIDER_LIST;
    }

    @Override
    public void clearCachedProviders() {
        // done!
    }

}
