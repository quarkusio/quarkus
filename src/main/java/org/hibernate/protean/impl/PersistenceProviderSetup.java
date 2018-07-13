package org.hibernate.protean.impl;

import javax.persistence.spi.PersistenceProviderResolverHolder;

public final class PersistenceProviderSetup {

	private PersistenceProviderSetup() {
		//not to be constructed
	}

	public static void registerPersistenceProvider() {
		PersistenceProviderResolverHolder.setPersistenceProviderResolver( new PPResolver() );
	}
}
