package io.quarkus.hibernate.reactive.panache.common.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.reactive.common.spi.Implementor;
import org.hibernate.reactive.context.Context.Key;
import org.hibernate.reactive.context.impl.BaseKey;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.Mutiny.Session;
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny.Transaction;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Static util methods for {@link Mutiny.Session}.
 */
public final class SessionOperations {

    private static final String ERROR_MSG = "Hibernate Reactive Panache requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such.";

    private static final ComputingCache<String, Key<Mutiny.Session>> SESSION_KEY_MAP = new ComputingCache<>(
            k -> createSessionKey(k));
    private static final ComputingCache<String, Mutiny.SessionFactory> SESSION_FACTORY_MAP = new ComputingCache<>(
            k -> createSessionFactory(k));

    private static SessionFactory createSessionFactory(String persistenceunitname) {
        SessionFactory sessionFactory;

        // Note that Mutiny.SessionFactory is @ApplicationScoped bean - it's safe to use the cached client proxy
        if (DEFAULT_PERSISTENCE_UNIT_NAME.equals(persistenceunitname)) {
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

    private static Key<Session> createSessionKey(String persistenceUnitName) {
        Implementor implementor = (Implementor) ClientProxy
                .unwrap(SESSION_FACTORY_MAP.getValue(persistenceUnitName));
        return new BaseKey<>(Session.class, implementor.getUuid());
    }

    // This key is used to indicate that reactive sessions should be opened lazily/on-demand (when needed) in the current vertx context
    private static final String SESSION_ON_DEMAND_KEY = "hibernate.reactive.panache.sessionOnDemand";

    // This key is used to keep track of the Set<String> sessions created on demand
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
                Set<String> onDemandSessionCreated = context.getLocal(SESSION_ON_DEMAND_OPENED_KEY);
                // Close only the sessions that have been created lazily (onDemand) in withSession
                // See this.getSession(String persistenceUnitName)
                if (onDemandSessionCreated != null) {
                    List<Uni<Void>> closedSessions = new ArrayList<>();
                    for (String s : onDemandSessionCreated) {
                        closedSessions.add(closeSession(s));
                    }
                    context.removeLocal(SESSION_ON_DEMAND_OPENED_KEY);
                    return Uni.combine().all().unis(closedSessions).discardItems();
                } else {
                    return Uni.createFrom().voidItem();
                }
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
        return withSession(DEFAULT_PERSISTENCE_UNIT_NAME, s -> s.withTransaction(t -> work.get()));
    }

    /**
     * Performs the work in the scope of a reactive transaction. An existing session is reused if possible.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withTransaction(String persistenceUnitName, Supplier<Uni<T>> work) {
        return withSession(persistenceUnitName, s -> s.withTransaction(t -> work.get()));
    }

    /**
     * Performs the work in the scope of a reactive transaction. An existing session is reused if possible.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withTransaction(Function<Transaction, Uni<T>> work) {
        return withSession(DEFAULT_PERSISTENCE_UNIT_NAME, s -> s.withTransaction(work));
    }

    /**
     * Performs the work in the scope of a named reactive session.
     * An existing session is reused if possible.
     *
     * @param <T>
     * @param persistenceUnitName
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withSession(String persistenceUnitName, Function<Session, Uni<T>> work) {
        Context context = vertxContext();
        Key<Mutiny.Session> key = SESSION_KEY_MAP.getValue(persistenceUnitName);
        Mutiny.Session current = context.getLocal(key);
        if (current != null && current.isOpen()) {
            // reactive session exists - reuse this session
            return work.apply(current);
        } else {
            // reactive session does not exist - open a new one and close it when the returned Uni completes
            return SESSION_FACTORY_MAP.getValue(persistenceUnitName)
                    .openSession()
                    .invoke(s -> context.putLocal(key, s))
                    .chain(work::apply)
                    .eventually(() -> closeSession(persistenceUnitName));
        }
    }

    /**
     * Performs the work in the scope of the default reactive session.
     * An existing session is reused if possible.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withSession(Function<Session, Uni<T>> work) {
        return withSession(DEFAULT_PERSISTENCE_UNIT_NAME, work);
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
        Key<Mutiny.Session> key = SESSION_KEY_MAP.getValue(persistenceUnitName);
        Mutiny.Session current = context.getLocal(key);
        if (current != null && current.isOpen()) {
            // reuse the existing reactive session
            return Uni.createFrom().item(current);
        } else {
            if (context.getLocal(SESSION_ON_DEMAND_KEY) != null) {
                // This will keep track of all on-demand opened sessions
                Set<String> onDemandSessionsCreated = context.getLocal(SESSION_ON_DEMAND_OPENED_KEY);
                if (onDemandSessionsCreated == null) {
                    onDemandSessionsCreated = new HashSet<>();
                    context.putLocal(SESSION_ON_DEMAND_OPENED_KEY, onDemandSessionsCreated);
                }

                if (onDemandSessionsCreated.contains(persistenceUnitName)) {
                    // a new reactive session is opened in a previous stage, reuse it
                    return Uni.createFrom().item(() -> getCurrentSession(persistenceUnitName));
                } else {
                    // open a new reactive session and store it in the vertx duplicated context
                    // the context was marked as "lazy" which means that the session will be eventually closed
                    onDemandSessionsCreated.add(persistenceUnitName);
                    return SESSION_FACTORY_MAP.getValue(persistenceUnitName).openSession()
                            .invoke(s -> context.putLocal(key, s));
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
        Mutiny.Session current = context.getLocal(SESSION_KEY_MAP.getValue(persistenceUnitName));
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
        Key<Mutiny.Session> key = SESSION_KEY_MAP.getValue(persistenceUnitName);
        Mutiny.Session current = context.getLocal(key);
        if (current != null && current.isOpen()) {
            return current.close().eventually(() -> context.removeLocal(key));
        }
        return Uni.createFrom().voidItem();
    }

    static void clear() {
        SESSION_FACTORY_MAP.clear();
        SESSION_KEY_MAP.clear();
    }
}
