package org.hibernate.protean.impl;

import java.util.Collections;
import java.util.List;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;

final class PPResolver implements PersistenceProviderResolver {

	private static List<PersistenceProvider> hardcodedProvidersList =
			Collections.<PersistenceProvider>singletonList( new FastbootHibernateProvider() );

	@Override
	public List<PersistenceProvider> getPersistenceProviders() {
		return hardcodedProvidersList;
	}

	@Override
	public void clearCachedProviders() {
		//done!
	}

}
