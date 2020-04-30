package io.quarkus.hibernate.rx.runtime;

import org.jboss.logging.Logger;

public class HibernateRx {

    static {
        // Override the JPA persistence unit resolver so to use our custom boot
        // strategy:
        RxPersistenceProviderSetup.registerPersistenceProvider();
    }

    public static void featureInit(boolean enabled) {
        if (enabled) {
            Logger.getLogger("org.hibernate.quarkus.feature").debug("Hibernate RX Features Enabled");
        }
    }

}
