package io.quarkus.hibernate.orm.runtime;

import org.jboss.logging.Logger;

public class Hibernate {

    static {
        // Override the JPA persistence unit resolver so to use our custom boot
        // strategy:
        PersistenceProviderSetup.registerPersistenceProvider();
    }

    public static void featureInit(boolean enabled) {
        if (enabled) {
            Logger.getLogger("org.hibernate.quarkus.feature").debug("Hibernate Features Enabled");
        }
    }

}
