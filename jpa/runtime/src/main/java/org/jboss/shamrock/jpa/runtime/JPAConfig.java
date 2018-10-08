package org.jboss.shamrock.jpa.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    private final AtomicReference<String> defaultPersistenceUnitName;

    public JPAConfig() {
        this.jtaEnabled = new AtomicBoolean();
        this.persistenceUnits = new HashMap<>();
        this.defaultPersistenceUnitName = new AtomicReference<String>();
    }

    void setJtaEnabled(boolean value) {
        jtaEnabled.set(value);
    }

    public EntityManagerFactory getEntityManagerFactory(String unitName) {
        if (unitName == null || unitName.isEmpty()) {
            if (persistenceUnits.size() == 1) {
                String defaultUnitName = defaultPersistenceUnitName.get();
                return defaultUnitName != null ? persistenceUnits.get(defaultUnitName)
                        : persistenceUnits.values().iterator().next();
            } else {
                throw new IllegalStateException("Unable to identify the default PU: " + persistenceUnits);
            }
        }
        return persistenceUnits.get(unitName);
    }

    void bootstrapPersistenceUnit(String unitName) {
        persistenceUnits.put(unitName, Persistence.createEntityManagerFactory(unitName));
    }

    void initDefaultPersistenceUnit() {
        if (persistenceUnits.size() == 1) {
            defaultPersistenceUnitName.set(persistenceUnits.keySet().iterator().next());
        }
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
