package org.hibernate.protean;

import org.hibernate.protean.impl.PersistenceProviderSetup;

public class Hibernate {

	static {
		PersistenceProviderSetup.registerPersistenceProvider();
	}

	public static void featureInit(){
		System.out.println("Hibernate Features Enabled");
	}

}
