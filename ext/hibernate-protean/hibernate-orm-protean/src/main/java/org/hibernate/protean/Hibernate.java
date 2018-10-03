package org.hibernate.protean;

import org.hibernate.protean.impl.PersistenceProviderSetup;
import org.jboss.logging.Logger;

public class Hibernate {

	static {
		PersistenceProviderSetup.registerPersistenceProvider();
	}

	public static void featureInit(){
		Logger.getLogger("org.hibernate.protean.feature").info("Hibernate Features Enabled");
	}

}
