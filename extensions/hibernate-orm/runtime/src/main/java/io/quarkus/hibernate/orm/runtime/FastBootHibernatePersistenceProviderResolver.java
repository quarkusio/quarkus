package io.quarkus.hibernate.orm.runtime;

import java.util.Collections;
import java.util.List;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;

final class FastBootHibernatePersistenceProviderResolver implements PersistenceProviderResolver {

    private static final List<PersistenceProvider> HARDCODED_PROVIDER_LIST = Collections
            .<PersistenceProvider> singletonList(new FastBootHibernatePersistenceProvider());

    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        return HARDCODED_PROVIDER_LIST;
    }

    @Override
    public void clearCachedProviders() {
        // done!
    }

}
