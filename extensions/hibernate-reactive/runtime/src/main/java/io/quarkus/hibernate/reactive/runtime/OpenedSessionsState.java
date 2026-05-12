package io.quarkus.hibernate.reactive.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.spi.context.storage.ContextLocal;

public abstract class OpenedSessionsState<T extends Mutiny.Closeable> {
    // This key is used to keep track of the Set<String> sessions created on demand
    private final String sessionOnDemandKey;

    private static final Logger LOG = Logger.getLogger(OpenedSessionsState.class);

    protected abstract T newSessionMethod(Mutiny.SessionFactory sessionFactory);

    protected abstract Class<T> getSessionType();

    protected abstract boolean isSessionOpen(T session);

    protected abstract Uni<Void> flushSession(T session);

    private final ContextLocal<ConcurrentHashMap<String, T>> sessionsLocal;

    private final ComputingCache<String, Mutiny.SessionFactory> sessionFactories = new ComputingCache<>(
            k -> createSessionFactory(k));

    protected OpenedSessionsState(ContextLocal sessionsLocal) {
        this.sessionsLocal = sessionsLocal;
        sessionOnDemandKey = "hibernate.reactive.openedSessionState." + getSessionType().getName();
    }

    public record SessionWithKey<T>(String persistenceUnitName, T session) {
    }

    public Optional<SessionWithKey<T>> getOpenedSession(Context context, String persistenceUnitName) {
        T current = context.getLocal(sessionsLocal, ConcurrentHashMap::new).get(persistenceUnitName);
        return Optional.ofNullable(current)
                .filter(this::isSessionOpen)
                .map(s -> new SessionWithKey<>(persistenceUnitName, s));
    }

    public Uni<Void> flushSession(Context context, String persistenceUnitName) {
        return getOpenedSession(context, persistenceUnitName)
                .map(session -> flushSession(session.session()))
                .orElse(Uni.createFrom().voidItem());
    }

    public Uni<Void> closeSession(Context context, String persistenceUnitName) {
        return getOpenedSession(context, persistenceUnitName)
                .map(session -> closeAndRemoveSession(context, session))
                .orElse(Uni.createFrom().voidItem());
    }

    public T createNewSession(String persistenceUnitName, Context context) {
        Mutiny.SessionFactory sessionFactory = sessionFactories.getValue(persistenceUnitName);
        T session = newSessionMethod(sessionFactory);

        storeSession(context, persistenceUnitName, session);
        return session;
    }

    private void storeSession(Context context, String persistenceUnitName, T session) {
        Set<String> openedSession = openedSessionContextSet(context);
        // open a new reactive session and store it in the vertx duplicated context
        // the context was marked as "lazy" which means that the session will be eventually closed
        openedSession.add(persistenceUnitName);
        context.getLocal(sessionsLocal, ConcurrentHashMap::new).put(persistenceUnitName, session);
    }

    private Set<String> openedSessionContextSet(Context context) {
        // This will keep track of all on-demand opened sessions
        Set<String> onDemandSessionsCreated = ContextLocals.<Set<String>> get(sessionOnDemandKey).orElse(null);
        if (onDemandSessionsCreated == null) {
            onDemandSessionsCreated = new HashSet<>();
            ContextLocals.put(sessionOnDemandKey, onDemandSessionsCreated);
        }
        return onDemandSessionsCreated;
    }

    private Uni<Void> closeAndRemoveSession(Context context, SessionWithKey<T> openSession) {
        return openSession.session().close()
                .eventually(() -> context.getLocal(sessionsLocal, ConcurrentHashMap::new)
                        .remove(openSession.persistenceUnitName()));
    }

    private static Mutiny.SessionFactory createSessionFactory(String persistenceunitname) {
        Mutiny.SessionFactory sessionFactory;

        // Note that Mutiny.SessionFactory is @ApplicationScoped bean - it's safe to use the cached client proxy
        if (DEFAULT_PERSISTENCE_UNIT_NAME.equals(persistenceunitname)) {
            sessionFactory = Arc.container().instance(Mutiny.SessionFactory.class).get();
        } else {
            sessionFactory = Arc.container().instance(Mutiny.SessionFactory.class,
                    new PersistenceUnit.PersistenceUnitLiteral(persistenceunitname)).get();
        }

        if (sessionFactory == null) {
            throw new IllegalStateException("Mutiny.SessionFactory bean not found");
        }
        return sessionFactory;
    }
}
