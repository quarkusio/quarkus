package io.quarkus.hibernate.reactive.runtime;

import org.jboss.logging.Logger;

public class HibernateReactive {

    public static void featureInit(boolean enabled) {
        // Override the JPA persistence unit resolver so to use our custom boot
        // strategy:
        ReactivePersistenceProviderSetup.registerStaticInitPersistenceProvider();

        if (enabled) {
            Logger.getLogger("org.hibernate.quarkus.feature").debug("Hibernate Reactive Features Enabled");
        }
    }

}
