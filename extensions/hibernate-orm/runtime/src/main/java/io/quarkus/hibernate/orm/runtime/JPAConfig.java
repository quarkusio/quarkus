package io.quarkus.hibernate.orm.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.smallrye.mutiny.tuples.Tuple2;

public class JPAConfig {

    private static final Logger LOGGER = Logger.getLogger(JPAConfig.class.getName());

    public static final String IS_REACTIVE_KEY = "isReactive";

    private final Map<PersistenceUnitKey, LazyPersistenceUnit> persistenceUnits = new HashMap<>();
    private final Set<String> deactivatedPersistenceUnitNames = new HashSet<>();
    private final boolean requestScopedSessionEnabled;

    @Inject
    public JPAConfig(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig) {
        for (Map.Entry<PersistenceUnitKey, QuarkusPersistenceUnitDescriptor> entry : PersistenceUnitsHolder
                .getPersistenceUnits().entrySet()) {
            QuarkusPersistenceUnitDescriptor descriptor = entry.getValue();
            String puName = descriptor.getName();
            if (descriptor.getProviderHelper().isActive(puName)) {
                persistenceUnits.put(entry.getKey(), new LazyPersistenceUnit(puName, descriptor.isReactive()));
            } else {
                deactivatedPersistenceUnitNames.add(puName);
            }
        }
        this.requestScopedSessionEnabled = hibernateOrmRuntimeConfig.requestScopedSessionEnabled();
    }

    void startAll() {
        List<CompletableFuture<?>> start = new ArrayList<>();
        //by using a dedicated thread for starting up the PU,
        //we work around https://github.com/quarkusio/quarkus/issues/17304 to some extent
        //as the main thread is now no longer polluted with ThreadLocals by default
        //this is not a complete fix, but will help as long as the test methods
        //don't access the datasource directly, but only over HTTP calls
        boolean moreThanOneThread = persistenceUnits.size() > 1;
        //start PUs in parallel, for faster startup
        for (Map.Entry<PersistenceUnitKey, LazyPersistenceUnit> i : persistenceUnits.entrySet()) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            start.add(future);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        i.getValue().get();
                        future.complete(null);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                }
            }, moreThanOneThread ? "JPA Startup Thread: " + i.getKey() : "JPA Startup Thread").start();
        }
        for (CompletableFuture<?> i : start) {
            try {
                i.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw e.getCause() instanceof RuntimeException ? (RuntimeException) e.getCause()
                        : new RuntimeException(e.getCause());
            }
        }
    }

    public List<Tuple2<String, EntityManagerFactory>> getEntityManagerFactories() {
        List<Tuple2<String, EntityManagerFactory>> allEntityManagerFactories = new ArrayList<>();
        for (LazyPersistenceUnit pu : persistenceUnits.values()) {
            allEntityManagerFactories.add(Tuple2.of(pu.name, getEntityManagerFactory(pu.name, pu.isReactive)));
        }

        return allEntityManagerFactories;
    }

    public EntityManagerFactory getEntityManagerFactory(String unitName, boolean reactive) {
        LazyPersistenceUnit lazyPersistenceUnit = null;
        if (unitName == null) {
            if (persistenceUnits.size() == 1) {
                lazyPersistenceUnit = persistenceUnits.values().iterator().next();
            }
        } else {
            lazyPersistenceUnit = persistenceUnits.get(new PersistenceUnitKey(unitName, reactive));
        }

        if (lazyPersistenceUnit == null) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "Unable to find an EntityManagerFactory for persistence unit '%s'.%s",
                            unitName,
                            deactivatedPersistenceUnitNames.contains(unitName)
                                    ? " This persistence unit is deactivated and should not have been created."
                                    : ""));
        }

        return lazyPersistenceUnit.get();
    }

    /**
     * Returns the registered, active persistence units.
     *
     * @return Set containing the names of all registered, actives persistence units.
     */
    public Set<String> getPersistenceUnits() {
        return persistenceUnits.keySet().stream().map(k -> k.name()).collect(Collectors.toSet());
    }

    /**
     * Returns the name of persistence units that were deactivated through configuration properties.
     *
     * @return Set containing the names of all persistence units that were deactivated through configuration properties.
     */
    public Set<String> getDeactivatedPersistenceUnitNames() {
        return deactivatedPersistenceUnitNames;
    }

    /**
     * Returns boolean value for enabling request scoped sessions
     */
    public boolean getRequestScopedSessionEnabled() {
        return this.requestScopedSessionEnabled;
    }

    void shutdown() {
        LOGGER.trace("Starting to shut down Hibernate ORM persistence units.");
        for (LazyPersistenceUnit factory : this.persistenceUnits.values()) {
            if (factory.isStarted()) {
                try {
                    LOGGER.tracef("Closing Hibernate ORM persistence unit: %s.", factory.name);
                    factory.close();
                } catch (Exception e) {
                    LOGGER.warn("Unable to close the EntityManagerFactory: " + factory, e);
                }
            } else {
                LOGGER.tracef("Skipping Hibernate ORM persistence unit, that failed to start: %s.", factory.name);
            }
        }
        this.persistenceUnits.clear();
        LOGGER.trace("Finished shutting down Hibernate ORM persistence units.");
    }

    static final class LazyPersistenceUnit {

        private final String name;
        private final boolean isReactive;
        private volatile EntityManagerFactory value;
        private volatile boolean closed = false;

        LazyPersistenceUnit(String name, boolean isReactive) {
            this.name = name;
            this.isReactive = isReactive;
        }

        EntityManagerFactory get() {
            if (value == null) {
                synchronized (this) {
                    if (closed) {
                        throw new IllegalStateException("Persistence unit is closed");
                    }
                    if (value == null) {
                        value = Persistence.createEntityManagerFactory(name,
                                Collections.singletonMap(IS_REACTIVE_KEY, isReactive));
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

        boolean isStarted() {
            return !closed && value != null;
        }
    }

}
