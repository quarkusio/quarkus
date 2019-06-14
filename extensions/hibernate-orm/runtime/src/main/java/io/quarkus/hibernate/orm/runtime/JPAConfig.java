package io.quarkus.hibernate.orm.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jboss.logging.Logger;

@Singleton
public class JPAConfig {

    private static final Logger LOGGER = Logger.getLogger(JPAConfig.class.getName());

    private final AtomicBoolean jtaEnabled;

    private final Map<String, LazyPersistenceUnit> persistenceUnits;

    private final AtomicReference<String> defaultPersistenceUnitName;

    public JPAConfig() {
        this.jtaEnabled = new AtomicBoolean();
        this.persistenceUnits = new ConcurrentHashMap<>();
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
        persistenceUnits.put(unitName, new LazyPersistenceUnit(unitName));
    }

    void startAll() {
        for (Map.Entry<String, LazyPersistenceUnit> i : persistenceUnits.entrySet()) {
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

    /**
     * Need to shutdown all instances of Hibernate ORM before the actual destroy event,
     * as it might need to use the datasources during shutdown.
     *
     * @param event ignored
     */
    void destroy(@BeforeDestroyed(ApplicationScoped.class) Object event) {
        for (LazyPersistenceUnit factory : persistenceUnits.values()) {
            try {
                factory.close();
            } catch (Exception e) {
                LOGGER.warn("Unable to close the EntityManagerFactory: " + factory, e);
            }
        }
    }

    @PreDestroy
    void destroy() {
        persistenceUnits.clear();
    }

    static final class LazyPersistenceUnit {

        private final String name;
        private volatile EntityManagerFactory value;
        private volatile boolean closed = false;

        LazyPersistenceUnit(String name) {
            this.name = name;
        }

        EntityManagerFactory get() {
            if (value == null) {
                synchronized (this) {
                    if (closed) {
                        throw new IllegalStateException("Persistence unit is closed");
                    }
                    if (value == null) {
                        value = Persistence.createEntityManagerFactory(name);
                    }
                }
            }
            return value;
        }

        public synchronized void close() {
            closed = true;
            EntityManagerFactory emf = this.value;
            this.value = null;
            if (emf != null) {
                emf.close();
            }
        }
    }

}
