package io.quarkus.hibernate.orm.runtime;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.MultiTenancyStrategy;
import org.jboss.logging.Logger;

@Singleton
public class JPAConfig {

    private static final Logger LOGGER = Logger.getLogger(JPAConfig.class.getName());

    private final AtomicReference<MultiTenancyStrategy> multiTenancyStrategy;

    private final AtomicReference<String> multiTenancySchemaDataSource;

    private final Map<String, LazyPersistenceUnit> persistenceUnits;

    private final AtomicReference<String> defaultPersistenceUnitName;

    public JPAConfig() {
        this.multiTenancyStrategy = new AtomicReference<MultiTenancyStrategy>();
        this.multiTenancySchemaDataSource = new AtomicReference<String>();
        this.persistenceUnits = new ConcurrentHashMap<>();
        this.defaultPersistenceUnitName = new AtomicReference<String>();
    }

    /**
     * Sets the strategy for multitenancy.
     *
     * @param strategy Strategy to use.
     */
    void setMultiTenancyStrategy(MultiTenancyStrategy strategy) {
        multiTenancyStrategy.set(strategy);
    }

    /**
     * Sets the name of the data source that should be used in case of {@link MultiTenancyStrategy#SCHEMA} approach.
     *
     * @param dataSourceName Name to use or {@literal null} for the default data source.
     */
    void setMultiTenancySchemaDataSource(String dataSourceName) {
        multiTenancySchemaDataSource.set(dataSourceName);
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
     * Returns the selected multitenancy strategy.
     *
     * @return Strategy to use.
     */
    public MultiTenancyStrategy getMultiTenancyStrategy() {
        return multiTenancyStrategy.get();
    }

    /**
     * Determines which data source should be used in case of {@link MultiTenancyStrategy#SCHEMA} approach.
     *
     * @return Data source name or {@link null} in case the default data source should be used.
     */
    public String getMultiTenancySchemaDataSource() {
        return multiTenancySchemaDataSource.get();
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
