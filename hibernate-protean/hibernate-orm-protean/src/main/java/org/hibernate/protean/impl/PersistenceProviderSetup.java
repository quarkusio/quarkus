package org.hibernate.protean.impl;

public final class PersistenceProviderSetup {

	private PersistenceProviderSetup() {
		//not to be constructed
	}

	public static void registerPersistenceProvider() {
		javax.persistence.spi.PersistenceProviderResolverHolder.setPersistenceProviderResolver( new PPResolver() );
	}

}
