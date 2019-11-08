package io.quarkus.hibernate.orm.runtime;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.logging.Logger;

public class Hibernate {

    static {
        // Override the JPA persistence unit resolver so to use our custom boot
        // strategy:
        PersistenceProviderSetup.registerPersistenceProvider();

        // We do our own enhancement during the compilation phase, so disable any
        // automatic entity enhancement by Hibernate ORM
        // This has to happen before Hibernate ORM classes are initialized: see
        // org.hibernate.cfg.Environment#BYTECODE_PROVIDER_INSTANCE
        System.setProperty(AvailableSettings.BYTECODE_PROVIDER,
                org.hibernate.cfg.Environment.BYTECODE_PROVIDER_NAME_NONE);
    }

    public static void featureInit(boolean enabled) {
        if (enabled) {
            Logger.getLogger("org.hibernate.quarkus.feature").debug("Hibernate Features Enabled");
        }
    }

}
