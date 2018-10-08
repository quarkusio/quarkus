package org.jboss.shamrock.jpa.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jboss.logging.Logger;


@Singleton
public class JPAConfig {

    private static final Logger LOGGER = Logger.getLogger(JPAConfig.class.getName());

    private final AtomicBoolean jtaEnabled;

    private final Map<String, EntityManagerFactory> persistenceUnits;

    public JPAConfig() {
        this.jtaEnabled = new AtomicBoolean();
        this.persistenceUnits = new HashMap<>();
    }

    void setJtaEnabled(boolean value) {
        jtaEnabled.set(value);
    }

    public EntityManagerFactory getEntityManagerFactory(String unitName) {
        if (unitName == null || unitName.isEmpty()) {
            if (persistenceUnits.size() == 1) {
                return persistenceUnits.values().iterator().next();
            } else {
                throw new IllegalStateException("Unable to identify the default PU: " + persistenceUnits);
            }
        }
        return persistenceUnits.get(unitName);
    }

    void bootstrapPersistenceUnit(String unitName) {
        persistenceUnits.put(unitName, Persistence.createEntityManagerFactory(unitName));
    }

    boolean isJtaEnabled() {
        return jtaEnabled.get();
    }

    @PreDestroy
    void destroy() {
        for (EntityManagerFactory factory : persistenceUnits.values()) {
            try {
                factory.close();
            } catch (Exception e) {
                LOGGER.warn("Unable to close the EntityManagerFactory: " + factory, e);
            }
        }
        persistenceUnits.clear();
    }

}
