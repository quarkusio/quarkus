package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.hibernate.orm.PersistenceUnit;
import org.hibernate.reactive.common.spi.Implementor;
import org.hibernate.reactive.context.Context.Key;
import org.hibernate.reactive.context.impl.BaseKey;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.Mutiny.Session;
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny.Transaction;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.impl.LazyValue;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

/**
 * Static util methods for {@link Mutiny.Session}.
 */
public final class SessionOperations {

    private static final String ERROR_MSG = "Hibernate Reactive Panache requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such.";

    private static final Map<String, LazyValue<Mutiny.SessionFactory>> SESSION_FACTORY_MAP = new HashMap<>();
    private static final Map<String, LazyValue<Key<Mutiny.Session>>> SESSION_KEY_MAP = new HashMap<>();

    static {
        SESSION_FACTORY_MAP.put(DEFAULT_PERSISTENCE_UNIT_NAME, createLazySessionFactory(DEFAULT_PERSISTENCE_UNIT_NAME));
        SESSION_KEY_MAP.put(DEFAULT_PERSISTENCE_UNIT_NAME, createLazySessionKey(DEFAULT_PERSISTENCE_UNIT_NAME));
    }

    private static LazyValue<SessionFactory> createLazySessionFactory(String persistenceunitname) {
        return new LazyValue<>(
                new Supplier<SessionFactory>() {
                    @Override
                    public SessionFactory get() {
                        SessionFactory sessionFactory;

                        // Note that Mutiny.SessionFactory is @ApplicationScoped bean - it's safe to use the cached client proxy
                        if(DEFAULT_PERSISTENCE_UNIT_NAME.equals(persistenceunitname)) {
                            sessionFactory = Arc.container().instance(SessionFactory.class).get();
                        } else {
                            sessionFactory = Arc.container().instance(SessionFactory.class,
                                    new PersistenceUnit.PersistenceUnitLiteral(persistenceunitname)).get();
                        }

                        if (sessionFactory == null) {
                            throw new IllegalStateException("Mutiny.SessionFactory bean not found");
                        }
                        return sessionFactory;
                    }
                });
    }

//    private static final LazyValue<Key<Mutiny.Session>> SESSION_KEY = createLazyDefaultSessionKey(DEFAULT_PERSISTENCE_UNIT_NAME);

    private static LazyValue<Key<Session>> createLazySessionKey(String persistenceUnitName) {
        return new LazyValue<>(
                new Supplier<Key<Session>>() {

                    @Override
                    public Key<Session> get() {
                        Implementor implementor = (Implementor) ClientProxy.unwrap(getOrCreateSessionFactory(persistenceUnitName));
                        return new BaseKey<>(Session.class, implementor.getUuid());
                    }
                });
    }

    private static SessionFactory getOrCreateSessionFactory(String persistenceUnitName) {
        return SESSION_FACTORY_MAP.computeIfAbsent(persistenceUnitName, k -> createLazySessionFactory(k)).get();
    }

    static Key<Mutiny.Session> getOrCreateSessionKey(String persistenceUnitName) {
        return SESSION_KEY_MAP.computeIfAbsent(persistenceUnitName, k -> createLazySessionKey(k)).get();
    }

    // This key is used to indicate that a reactive session should be opened lazily (when needed) in the current vertx context
    private static final String SESSION_ON_DEMAND_KEY = "hibernate.reactive.panache.sessionOnDemand";
    private static final String SESSION_ON_DEMAND_OPENED_KEY = "hibernate.reactive.panache.sessionOnDemandOpened";

    /**
     * Marks the current vertx duplicated context as "lazy" which indicates that a reactive session should be opened lazily if
     * needed. The opened session is eventually closed and the marking key is removed when the provided {@link Uni} completes.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     * @see #getSession(String)
     */
    static <T> Uni<T> withSessionOnDemand(Supplier<Uni<T>> work) {
        Context context = vertxContext();
        if (context.getLocal(SESSION_ON_DEMAND_KEY) != null) {
            // context already marked - no need to set the key and close the session
            return work.get();
        } else {
            // mark the lazy session
            context.putLocal(SESSION_ON_DEMAND_KEY, true);
            // perform the work and eventually close the session and remove the key
            return work.get().eventually(() -> {
                context.removeLocal(SESSION_ON_DEMAND_KEY);
                context.removeLocal(SESSION_ON_DEMAND_OPENED_KEY);
                // TODO Luca this is just wrong
                return closeSession(DEFAULT_PERSISTENCE_UNIT_NAME);
            });
        }
    }

    /**
     * Performs the work in the scope of a reactive transaction. An existing session is reused if possible.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withTransaction(Supplier<Uni<T>> work) {
        return withSession(s -> s.withTransaction(t -> work.get()), DEFAULT_PERSISTENCE_UNIT_NAME);
    }

    /**
     * Performs the work in the scope of a reactive transaction. An existing session is reused if possible.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withTransaction(Function<Transaction, Uni<T>> work) {
        return withSession(s -> s.withTransaction(work), DEFAULT_PERSISTENCE_UNIT_NAME);
    }

    /**
     * Performs the work in the scope of a reactive session. An existing session is reused if possible.
     *
     * @param <T>
     * @param work
     * @param persistenceUnitName
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withSession(Function<Mutiny.Session, Uni<T>> work, String persistenceUnitName) {
        Context context = vertxContext();
        Key<Mutiny.Session> key = getOrCreateSessionKey(persistenceUnitName);
        Mutiny.Session current = context.getLocal(key);
        if (current != null && current.isOpen()) {
            // reactive session exists - reuse this session
            return work.apply(current);
        } else {
            // reactive session does not exist - open a new one and close it when the returned Uni completes
            return getOrCreateSessionFactory(persistenceUnitName)
                    .openSession()
                    .invoke(s -> context.putLocal(key, s))
                    .chain(work::apply)
                    .eventually(() -> closeSession(persistenceUnitName));
        }
    }

    /**
     * If there is a reactive session stored in the current Vert.x duplicated context then this session is reused.
     * <p>
     * However, if there is no reactive session found then:
     * <ol>
     * <li>if the current vertx duplicated context is marked as "lazy" then a new session is opened and stored it in the
     * context</li>
     * <li>otherwise an exception thrown</li>
     * </ol>
     *
     * @throws IllegalStateException If no reactive session was found in the context and the context was not marked to open a
     *         new session lazily
     * @return the {@link Mutiny.Session}
     */
    public static Uni<Mutiny.Session> getSession() {
        return getSession(DEFAULT_PERSISTENCE_UNIT_NAME);
    }

    public static Uni<Mutiny.Session> getSession(String persistenceUnitName) {
        Context context = vertxContext();
        Key<Mutiny.Session> key = getOrCreateSessionKey(persistenceUnitName);
        Mutiny.Session current = context.getLocal(key);
        if (current != null && current.isOpen()) {
            // reuse the existing reactive session
            return Uni.createFrom().item(current);
        } else {
            if (context.getLocal(SESSION_ON_DEMAND_KEY) != null) {
                if (context.getLocal(SESSION_ON_DEMAND_OPENED_KEY) != null) {
                    // a new reactive session is opened in a previous stage
                    return Uni.createFrom().item(() -> getCurrentSession(persistenceUnitName));
                } else {
                    // open a new reactive session and store it in the vertx duplicated context
                    // the context was marked as "lazy" which means that the session will be eventually closed
                    context.putLocal(SESSION_ON_DEMAND_OPENED_KEY, true);
                    return getOrCreateSessionFactory(persistenceUnitName).openSession().invoke(s -> {
                        context.putLocal(key, s);
                    });
                }
            } else {
                throw new IllegalStateException("No current Mutiny.Session found"
                        + "\n\t- no reactive session was found in the Vert.x context and the context was not marked to open a new session lazily"
                        + "\n\t- a session is opened automatically for JAX-RS resource methods annotated with an HTTP method (@GET, @POST, etc.); inherited annotations are not taken into account"
                        + "\n\t- you may need to annotate the business method with @WithSession or @WithTransaction");
            }
        }
    }

    /**
     * @return the current reactive session stored in the context, or {@code null} if no session exists
     */
    public static Mutiny.Session getCurrentSession(String persistenceUnitName) {
        Context context = vertxContext();
        Mutiny.Session current = context.getLocal(getOrCreateSessionKey(persistenceUnitName));
        if (current != null && current.isOpen()) {
            return current;
        }
        return null;
    }

    /**
     *
     * @return the current vertx duplicated context
     * @throws IllegalStateException If no vertx context is found or is not a safe context as mandated by the
     *         {@link VertxContextSafetyToggle}
     */
    private static Context vertxContext() {
        Context context = Vertx.currentContext();
        if (context != null) {
            VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG, ERROR_MSG);
            return context;
        } else {
            throw new IllegalStateException("No current Vertx context found");
        }
    }

    static Uni<Void> closeSession(String persistenceUnitName) {
        Context context = vertxContext();
        Key<Mutiny.Session> key = getOrCreateSessionKey(persistenceUnitName);
        Mutiny.Session current = context.getLocal(key);
        if (current != null && current.isOpen()) {
            return current.close().eventually(() -> context.removeLocal(key));
        }
        return Uni.createFrom().voidItem();
    }

    static void clear() {
        SESSION_FACTORY_MAP.values().forEach(v -> v.clear());
        SESSION_KEY_MAP.values().forEach(v -> v.clear());
//        SESSION_FACTORY.clear();
//        SESSION_KEY.clear();
    }
}
