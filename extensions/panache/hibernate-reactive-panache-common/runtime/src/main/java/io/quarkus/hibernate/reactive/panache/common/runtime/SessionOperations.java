package io.quarkus.hibernate.reactive.panache.common.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.SESSION_ON_DEMAND_KEY;
import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.Mutiny.Transaction;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.hibernate.reactive.runtime.OpenedSessionsState;
import io.quarkus.reactive.transaction.runtime.pool.TransactionalContextPool;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlConnection;

/**
 * Static util methods for {@link Mutiny.Session}.
 */
public final class SessionOperations {

    private static final Logger LOG = Logger.getLogger(SessionOperations.class);

    private static final String ERROR_MSG = "Hibernate Reactive Panache requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such.";

    private static final OpenedSessionsState<Mutiny.Session> SESSIONS = HibernateReactiveRecorder.OPENED_SESSIONS_STATE;
    private static final OpenedSessionsState<Mutiny.StatelessSession> STATELESS_SESSIONS = HibernateReactiveRecorder.OPENED_SESSIONS_STATE_STATELESS;

    // This key is used to keep track of the Set<String> sessions (managed or stateless) created on demand
    private static final String SESSION_ON_DEMAND_OPENED_KEY = "hibernate.reactive.panache.sessionOnDemandOpened";

    /**
     * Marks the current vertx duplicated context as "lazy" which indicates that a reactive session should be opened lazily if
     * needed. The opened session is eventually closed and the marking key is removed when the provided {@link Uni} completes.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     * @see #getSession(String)
     * @see #getStatelessSession(String)
     */
    static <T> Uni<T> withSessionOnDemand(Supplier<Uni<T>> work) {
        Context context = vertxContext();

        if (context.getLocal(TRANSACTIONAL_METHOD_KEY) != null) {
            return Uni.createFrom().failure(
                    new UnsupportedOperationException(
                            "Calling a method annotated with @WithSessionOnDemand from a method annotated with @Transactional is not supported. "
                                    + "Use either @Transactional or @WithSessionOnDemand/@WithSession/@WithTransaction, "
                                    + "but not both, throughout your whole application."));
        }

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
     * Performs the work in the scope of a reactive transaction. An existing managed session is reused if possible.
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
     * Performs the work in the scope of a reactive transaction. An existing stateless session is reused if possible.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withStatelessTransaction(Supplier<Uni<T>> work) {
        return withStatelessSession(s -> s.withTransaction(t -> work.get()));
    }

    /**
     * Performs the work in the scope of a reactive transaction. An existing stateless session is reused if possible.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withStatelessTransaction(String persistenceUnitName, Supplier<Uni<T>> work) {
        return withStatelessSession(persistenceUnitName, s -> s.withTransaction(t -> work.get()));
    }

    /**
     * Performs the work in the scope of a reactive transaction. An existing stateless session is reused if possible.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withStatelessTransaction(Function<Transaction, Uni<T>> work) {
        return withStatelessSession(DEFAULT_PERSISTENCE_UNIT_NAME, s -> s.withTransaction(work));
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
    public static <T> Uni<T> withSession(String persistenceUnitName, Function<Mutiny.Session, Uni<T>> work) {
        Context context = vertxContext();
        // First make sure we don't already have an opened stateless session
        Uni<T> error = checkNoStatelessSession(context, persistenceUnitName);
        if (error != null) {
            return error;
        }
        Optional<OpenedSessionsState.SessionWithKey<Mutiny.Session>> opened = SESSIONS.getOpenedSession(context,
                persistenceUnitName);
        if (opened.isPresent()) {
            return work.apply(opened.get().session());
        } else {
            Mutiny.Session session = SESSIONS.createNewSession(persistenceUnitName, context);
            LOG.debugf("Opening lazy managed session for Persistence Unit '%s'", persistenceUnitName);
            return work.apply(session)
                    .eventually(() -> SESSIONS.closeSession(context, persistenceUnitName));
        }
    }

    private static <T> Uni<T> checkNoStatelessSession(Context context, String persistenceUnitName) {
        Optional<OpenedSessionsState.SessionWithKey<Mutiny.StatelessSession>> opened = STATELESS_SESSIONS
                .getOpenedSession(context, persistenceUnitName);
        if (opened.isPresent()) {
            return Uni.createFrom().failure(
                    new IllegalStateException("There is already a stateless session opened for this persistence unit"));
        }
        return null;
    }

    /**
     * Performs the work in the scope of the default reactive session.
     * An existing session is reused if possible.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withSession(Function<Mutiny.Session, Uni<T>> work) {
        return withSession(DEFAULT_PERSISTENCE_UNIT_NAME, work);
    }

    /**
     * Performs the work in the scope of a named reactive stateless session.
     * An existing stateless session is reused if possible.
     *
     * @param <T>
     * @param persistenceUnitName
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withStatelessSession(String persistenceUnitName,
            Function<Mutiny.StatelessSession, Uni<T>> work) {
        Context context = vertxContext();
        // First make sure we don't already have an opened managed session
        Uni<T> error = checkNoManagedSession(context, persistenceUnitName);
        if (error != null) {
            return error;
        }
        Optional<OpenedSessionsState.SessionWithKey<Mutiny.StatelessSession>> opened = STATELESS_SESSIONS
                .getOpenedSession(context, persistenceUnitName);
        if (opened.isPresent()) {
            return work.apply(opened.get().session());
        } else {
            Mutiny.StatelessSession session = STATELESS_SESSIONS.createNewSession(persistenceUnitName, context);
            LOG.debugf("Opening lazy stateless session for Persistence Unit '%s'", persistenceUnitName);
            return work.apply(session)
                    .eventually(() -> STATELESS_SESSIONS.closeSession(context, persistenceUnitName));
        }
    }

    private static <T> Uni<T> checkNoManagedSession(Context context, String persistenceUnitName) {
        Optional<OpenedSessionsState.SessionWithKey<Mutiny.Session>> opened = SESSIONS.getOpenedSession(context,
                persistenceUnitName);
        if (opened.isPresent()) {
            return Uni.createFrom()
                    .failure(new IllegalStateException("There is already a managed session opened for this persistence unit"));
        }
        return null;
    }

    /**
     * Performs the work in the scope of the default reactive stateless session.
     * An existing session is reused if possible.
     *
     * @param <T>
     * @param work
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withStatelessSession(Function<Mutiny.StatelessSession, Uni<T>> work) {
        return withStatelessSession(DEFAULT_PERSISTENCE_UNIT_NAME, work);
    }

    /**
     * If there is a reactive session stored in the current Vert.x duplicated context then this session is reused.
     * <p>
     * However, if there is no reactive session found then:
     * <ol>
     * <li>if the current vertx duplicated context is marked as "lazy" then a new session is opened and stored it in the
     * context</li>
     * <li>if the current context is marked as transactional then a new session is created via the shared session state</li>
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
        // First make sure we don't already have an opened stateless session
        Uni<Mutiny.Session> error = checkNoStatelessSession(context, persistenceUnitName);
        if (error != null) {
            return error;
        }
        Optional<OpenedSessionsState.SessionWithKey<Mutiny.Session>> opened = SESSIONS.getOpenedSession(context,
                persistenceUnitName);
        if (opened.isPresent()) {
            return Uni.createFrom().item(opened.get().session());
        } else if (context.getLocal(SESSION_ON_DEMAND_KEY) != null) {
            trackOnDemandSession(context, persistenceUnitName);
            return Uni.createFrom()
                    .item(() -> HibernateReactiveRecorder.getSession(persistenceUnitName, SESSION_ON_DEMAND_KEY));
        } else if (context.getLocal(TRANSACTIONAL_METHOD_KEY) != null) {
            return Uni.createFrom()
                    .item(() -> HibernateReactiveRecorder.getSession(persistenceUnitName, TRANSACTIONAL_METHOD_KEY));
        } else {
            throw new IllegalStateException("No current Mutiny.Session found"
                    + "\n\t- no reactive session was found in the Vert.x context and the context was not marked to open a new session lazily"
                    + "\n\t- a session is opened automatically for JAX-RS resource methods annotated with an HTTP method (@GET, @POST, etc.); inherited annotations are not taken into account"
                    + "\n\t- you may need to annotate the business method with @WithSession or @WithTransaction");
        }
    }

    /**
     * If there is a reactive stateless session stored in the current Vert.x duplicated context then this stateless session is
     * reused.
     * <p>
     * However, if there is no reactive stateless session found then:
     * <ol>
     * <li>if the current vertx duplicated context is marked as "lazy" then a new stateless session is opened and stored it in
     * the
     * context</li>
     * <li>if the current context is marked as transactional then a new stateless session is created via the shared session
     * state</li>
     * <li>otherwise an exception thrown</li>
     * </ol>
     *
     * @throws IllegalStateException If no reactive stateless session was found in the context and the context was not marked to
     *         open a
     *         new session lazily
     * @return the {@link Mutiny.Session}
     */
    public static Uni<Mutiny.StatelessSession> getStatelessSession() {
        return getStatelessSession(DEFAULT_PERSISTENCE_UNIT_NAME);
    }

    public static Uni<Mutiny.StatelessSession> getStatelessSession(String persistenceUnitName) {
        Context context = vertxContext();
        // First make sure we don't already have an opened managed session
        Uni<Mutiny.StatelessSession> error = checkNoManagedSession(context, persistenceUnitName);
        if (error != null) {
            return error;
        }
        Optional<OpenedSessionsState.SessionWithKey<Mutiny.StatelessSession>> opened = STATELESS_SESSIONS
                .getOpenedSession(context, persistenceUnitName);
        if (opened.isPresent()) {
            return Uni.createFrom().item(opened.get().session());
        } else if (context.getLocal(SESSION_ON_DEMAND_KEY) != null) {
            trackOnDemandSession(context, persistenceUnitName);
            return Uni.createFrom()
                    .item(() -> HibernateReactiveRecorder.getStatelessSession(persistenceUnitName, SESSION_ON_DEMAND_KEY));
        } else if (context.getLocal(TRANSACTIONAL_METHOD_KEY) != null) {
            return Uni.createFrom()
                    .item(() -> HibernateReactiveRecorder.getStatelessSession(persistenceUnitName, TRANSACTIONAL_METHOD_KEY));
        } else {
            throw new IllegalStateException("No current Mutiny.StatelessSession found"
                    + "\n\t- no reactive stateless session was found in the Vert.x context and the context was not marked to open a new session lazily"
                    + "\n\t- a stateless session is opened automatically for JAX-RS resource methods annotated with an HTTP method (@GET, @POST, etc.); inherited annotations are not taken into account"
                    + "\n\t- you may need to annotate the business method with @WithStatelessSession or @WithStatelessTransaction");
        }
    }

    /**
     * @return the current reactive session stored in the context, or {@code null} if no session exists
     */
    public static Mutiny.Session getCurrentSession(String persistenceUnitName) {
        Context context = vertxContext();
        return SESSIONS.getOpenedSession(context, persistenceUnitName)
                .map(OpenedSessionsState.SessionWithKey::session)
                .orElse(null);
    }

    /**
     * @return the current reactive stateless session stored in the context, or {@code null} if no stateless session exists
     */
    public static Mutiny.StatelessSession getCurrentStatelessSession(String persistenceUnitName) {
        Context context = vertxContext();
        return STATELESS_SESSIONS.getOpenedSession(context, persistenceUnitName)
                .map(OpenedSessionsState.SessionWithKey::session)
                .orElse(null);
    }

    private static void trackOnDemandSession(Context context, String persistenceUnitName) {
        Set<String> onDemandSessionsCreated = context.getLocal(SESSION_ON_DEMAND_OPENED_KEY);
        if (onDemandSessionsCreated == null) {
            onDemandSessionsCreated = new HashSet<>();
            context.putLocal(SESSION_ON_DEMAND_OPENED_KEY, onDemandSessionsCreated);
        }
        onDemandSessionsCreated.add(persistenceUnitName);
    }

    /**
     *
     * @return the current vertx duplicated context
     * @throws IllegalStateException If no vertx context is found or is not a safe context as mandated by the
     *         {@link VertxContextSafetyToggle}
     */
    public static Uni<Mutiny.Transaction> currentTransaction() {
        return getSession().map(session -> {
            Mutiny.Transaction tx = session.currentTransaction();
            if (tx != null) {
                return tx;
            }
            Future<? extends SqlConnection> connectionFuture = TransactionalContextPool.getCurrentConnectionFromVertxContext();
            if (connectionFuture != null && connectionFuture.succeeded()) {
                io.vertx.sqlclient.Transaction vertxTx = connectionFuture.result().transaction();
                if (vertxTx != null) {
                    return new VertxTransactionWrapper(vertxTx);
                }
            }
            return null;
        });
    }

    public static Context vertxContext() {
        Context context = Vertx.currentContext();
        if (context != null) {
            VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG, ERROR_MSG);
            return context;
        } else {
            throw new IllegalStateException("No current Vertx context found");
        }
    }

    /**
     * Close any session open for that persistence unit (stateless or managed, there can be only one opened at a time)
     */
    static Uni<Void> closeSession(String persistenceUnitName) {
        LOG.debugf("Closing session for Persistence Unit '%s'", persistenceUnitName);
        Context context = vertxContext();
        return SESSIONS.closeSession(context, persistenceUnitName)
                .chain(() -> STATELESS_SESSIONS.closeSession(context, persistenceUnitName));
    }

    static void clear() {
    }
}
