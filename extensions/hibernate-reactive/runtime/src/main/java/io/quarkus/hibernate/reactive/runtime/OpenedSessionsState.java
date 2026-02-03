package io.quarkus.hibernate.reactive.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.reactive.common.spi.Implementor;
import org.hibernate.reactive.context.Context.Key;
import org.hibernate.reactive.context.impl.BaseKey;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public abstract class OpenedSessionsState<T extends Mutiny.Closeable> {
    // This key is used to keep track of the Set<String> sessions created on demand
    private final String sessionOnDemandKey;

    private static final Logger LOG = Logger.getLogger(OpenedSessionsState.class);

    protected abstract T newSessionMethod(Mutiny.SessionFactory sessionFactory);

    protected abstract Class<T> getSessionType();

    protected abstract boolean isSessionOpen(T session);

    private final ComputingCache<String, Key<T>> sessionKeys = new ComputingCache<>(
            k -> createKeyForSessionType(k));

    private final ComputingCache<String, Mutiny.SessionFactory> sessionFactories = new ComputingCache<>(
            k -> createSessionFactory(k));

    protected OpenedSessionsState() {
        sessionOnDemandKey = "hibernate.reactive.openedSessionState." + getSessionType().getName();
    }

    public record SessionWithKey<T>(org.hibernate.reactive.context.Context.Key<T> key, T session) {
    }

    public Optional<SessionWithKey<T>> getOpenedSession(Context context, String persistenceUnitName) {
        Key<T> sessionKey = sessionKeys.getValue(persistenceUnitName);
        return getOpenedSession(context, sessionKey);
    }

    private Optional<SessionWithKey<T>> getOpenedSession(Context context, Key<T> sessionKey) {
        T current = context.getLocal(sessionKey);
        return Optional.ofNullable(current)
                .filter(s -> isSessionOpen(s))
                .map(s -> new SessionWithKey<>(sessionKey, s));
    }

    public Uni<Void> closeAllOpenedSessions(Context context) {
        Set<String> onDemandSessionCreated = openedSessionContextSet(context);
        if (onDemandSessionCreated.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        List<Uni<Void>> closedSessionsUnis = new ArrayList<>();
        for (String sessionName : onDemandSessionCreated) {

            Uni<Void> closeSessionUni = getOpenedSession(context, sessionName)
                    .map(session -> closeAndRemoveSession(context, session))
                    .orElse(Uni.createFrom().voidItem());

            closedSessionsUnis.add(closeSessionUni);
        }
        context.removeLocal(sessionOnDemandKey);
        return Uni.combine().all().unis(closedSessionsUnis).discardItems();
    }

    public T createNewSession(String persistenceUnitName, Context context) {
        Mutiny.SessionFactory sessionFactory = sessionFactories.getValue(persistenceUnitName);
        T session = newSessionMethod(sessionFactory);
        Key<T> sessionKey = sessionKeys.getValue(persistenceUnitName);

        storeSession(context, persistenceUnitName, sessionKey, session);
        return session;
    }

    private void storeSession(Context context, String persistenceUnitName, Key<T> sessionKey, T session) {
        Set<String> openedSession = openedSessionContextSet(context);
        // open a new reactive session and store it in the vertx duplicated context
        // the context was marked as "lazy" which means that the session will be eventually closed
        openedSession.add(persistenceUnitName);
        context.putLocal(sessionKey, session);
    }

    private Set<String> openedSessionContextSet(Context context) {
        // This will keep track of all on-demand opened sessions
        Set<String> onDemandSessionsCreated = context.getLocal(sessionOnDemandKey);
        if (onDemandSessionsCreated == null) {
            onDemandSessionsCreated = new HashSet<>();
            context.putLocal(sessionOnDemandKey, onDemandSessionsCreated);
        }
        return onDemandSessionsCreated;
    }

    private Uni<Void> closeAndRemoveSession(Context context, SessionWithKey<T> openSession) {
        // Force flushing in case there are pending operations in the session
        return eventuallyFlush(openSession.session)
                .chain(() -> openSession.session.close())
                .eventually(() -> context.removeLocal(openSession.key));
    }

    private static <T extends Mutiny.Closeable> Uni<Void> eventuallyFlush(T session) {
        if (session instanceof Mutiny.Session) {
            LOG.tracef("Flushing the session");
            return ((Mutiny.Session) session).flush();
        } else {
            return Uni.createFrom().voidItem();
        }
    }

    private Key<T> createKeyForSessionType(String persistenceUnitName) {
        Mutiny.SessionFactory value = createSessionFactory(persistenceUnitName);
        Implementor implementor = (Implementor) ClientProxy.unwrap(value);
        return new BaseKey<>(getSessionType(), implementor.getUuid());
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
