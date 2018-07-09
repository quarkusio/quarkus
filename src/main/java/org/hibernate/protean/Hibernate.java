package org.hibernate.protean;

import javax.persistence.spi.PersistenceProviderResolverHolder;

import org.hibernate.protean.impl.PPResolver;

public class Hibernate {

	static {
		PersistenceProviderResolverHolder.setPersistenceProviderResolver( PPResolver.INSTANCE );
	}

	public static void featureInit(){
		System.out.println("Hibernate Features Enabled");
	}

}
