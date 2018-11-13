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

    private final Map<String, LazyPu> persistenceUnits;

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
                return defaultUnitName != null ? persistenceUnits.get(defaultUnitName).get()
                        : persistenceUnits.values().iterator().next().get();
            } else {
                throw new IllegalStateException("Unable to identify the default PU: " + persistenceUnits);
            }
        }
        return persistenceUnits.get(unitName).get();
    }

    void registerPersistenceUnit(String unitName) {
        persistenceUnits.put(unitName, new LazyPu(unitName));
    }

    void startAll() {
        for(Map.Entry<String, LazyPu> i : persistenceUnits.entrySet()) {
            i.getValue().get();
        }
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
        for (LazyPu factory : persistenceUnits.values()) {
            try {
                factory.get().close();
            } catch (Exception e) {
                LOGGER.warn("Unable to close the EntityManagerFactory: " + factory, e);
            }
        }
        persistenceUnits.clear();
    }

    static final class LazyPu {

        private final String name;
        private volatile EntityManagerFactory value;

        LazyPu(String name) {
            this.name = name;
        }

        EntityManagerFactory get() {
            if(value == null) {
                synchronized (this) {
                    if(value == null) {
                        value = Persistence.createEntityManagerFactory(name);
                    }
                }
            }
            return value;
        }

    }

}
