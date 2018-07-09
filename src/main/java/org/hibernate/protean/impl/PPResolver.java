package org.hibernate.protean.impl;

import java.util.Collections;
import java.util.List;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;

public final class PPResolver implements PersistenceProviderResolver {

	public static PPResolver INSTANCE = new PPResolver();
	private PPResolver() {}

	@Override
	public List<PersistenceProvider> getPersistenceProviders() {
		return Collections.<PersistenceProvider>singletonList( new FastbootHibernateProvider() );
	}

	@Override
	public void clearCachedProviders() {
		//done!
	}

}
