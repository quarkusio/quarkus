package io.quarkus.hibernate.orm.runtime;

import org.jboss.logging.Logger;

public class Hibernate {

    public static void featureInit(boolean enabled) {
        // Override the JPA persistence unit resolver so to use our custom boot
        // strategy:
        PersistenceProviderSetup.registerStaticInitPersistenceProvider();

        if (enabled) {
            Logger.getLogger("org.hibernate.quarkus.feature").debug("Hibernate Features Enabled");
        }
    }

}
