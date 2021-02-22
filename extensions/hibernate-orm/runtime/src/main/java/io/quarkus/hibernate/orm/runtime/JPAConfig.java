package io.quarkus.hibernate.orm.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jboss.logging.Logger;

@Singleton
public class JPAConfig {

    private static final Logger LOGGER = Logger.getLogger(JPAConfig.class.getName());

    private final Map<String, Set<String>> entityPersistenceUnitMapping;

    private final Map<String, LazyPersistenceUnit> persistenceUnits;

    @Inject
    public JPAConfig(JPAConfigSupport jpaConfigSupport) {
        this.entityPersistenceUnitMapping = Collections.unmodifiableMap(jpaConfigSupport.entityPersistenceUnitMapping);

        Map<String, LazyPersistenceUnit> persistenceUnitsBuilder = new HashMap<>();
        for (String persistenceUnitName : jpaConfigSupport.persistenceUnitNames) {
            persistenceUnitsBuilder.put(persistenceUnitName, new LazyPersistenceUnit(persistenceUnitName));
        }
        this.persistenceUnits = persistenceUnitsBuilder;
    }

    void startAll() {
        for (Map.Entry<String, LazyPersistenceUnit> i : persistenceUnits.entrySet()) {
            i.getValue().get();
        }
    }

    public EntityManagerFactory getEntityManagerFactory(String unitName) {
        LazyPersistenceUnit lazyPersistenceUnit = null;
        if (unitName == null) {
            if (persistenceUnits.size() == 1) {
                lazyPersistenceUnit = persistenceUnits.values().iterator().next();
            }
        } else {
            lazyPersistenceUnit = persistenceUnits.get(unitName);
        }

        if (lazyPersistenceUnit == null) {
            throw new IllegalArgumentException(
                    String.format("Unable to find an EntityManagerFactory for persistence unit '%s'", unitName));
        }

        return lazyPersistenceUnit.get();
    }

    /**
     * Returns the registered persistence units.
     *
     * @return Set containing the names of all registered persistence units.
     */
    public Set<String> getPersistenceUnits() {
        return persistenceUnits.keySet();
    }

    /**
     * Returns the set of persistence units an entity is attached to.
     */
    public Set<String> getPersistenceUnitsForEntity(String entityClass) {
        return entityPersistenceUnitMapping.getOrDefault(entityClass, Collections.emptySet());
    }

    /**
     * Need to shutdown all instances of Hibernate ORM before the actual destroy event,
     * as it might need to use the datasources during shutdown.
     *
     * @param event ignored
     */
    void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
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
