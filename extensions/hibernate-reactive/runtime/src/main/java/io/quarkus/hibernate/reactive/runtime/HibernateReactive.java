package io.quarkus.hibernate.reactive.runtime;

import org.jboss.logging.Logger;

public class HibernateReactive {

    static {
        // Override the JPA persistence unit resolver so to use our custom boot
        // strategy:
        ReactivePersistenceProviderSetup.registerPersistenceProvider();
    }

    public static void featureInit(boolean enabled) {
        if (enabled) {
            Logger.getLogger("org.hibernate.quarkus.feature").debug("Hibernate Reactive Features Enabled");
        }
    }

}
