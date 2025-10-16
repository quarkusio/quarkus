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
import org.hibernate.reactive.mutiny.impl.MutinySessionImpl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public class OpenedSessionsState {
    // This key is used to keep track of the Set<String> sessions created on demand
    private static final String SESSIONS_ON_DEMAND_OPENED_KEY = "hibernate.reactive.panache.sessionOnDemandOpened";

    private final ComputingCache<String, Key<Mutiny.Session>> sessionKeys = new ComputingCache<>(
            k -> createSessionFactoryAndStoreKey(k));

    private final ComputingCache<String, Mutiny.SessionFactory> sessionFactories = new ComputingCache<>(
            k -> createSessionFactory(k));

    public record SessionWithKey(org.hibernate.reactive.context.Context.Key<Mutiny.Session> key, Mutiny.Session session) {

    }

    public Optional<SessionWithKey> getOpenedSession(Context context, String persistenceUnitName) {
        Key<Mutiny.Session> sessionKey = sessionKeys.getValue(persistenceUnitName);
        return getOpenedSession(context, sessionKey);
    }

    private static Optional<SessionWithKey> getOpenedSession(Context context, Key<Mutiny.Session> sessionKey) {
        Mutiny.Session current = context.getLocal(sessionKey);
        return Optional.ofNullable(current)
                .filter(s -> s.isOpen())
                .map(s -> new SessionWithKey(sessionKey, s));
    }

    public Uni<Void> closeAllOpenedSessions(Context context) {
        Set<String> onDemandSessionCreated = openedSessionContextSet(context);
        if (onDemandSessionCreated.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        List<Uni<Void>> closedSessionsUnis = new ArrayList<>();
        for (String s : onDemandSessionCreated) {
            Optional<SessionWithKey> openedSession = getOpenedSession(context, s);
            closedSessionsUnis.add(closeAndRemoveSession(context, openedSession));
        }
        context.removeLocal(SESSIONS_ON_DEMAND_OPENED_KEY);
        return Uni.combine().all().unis(closedSessionsUnis).discardItems();
    }

    public Mutiny.Session createNewSession(String persistenceUnitName, Context context) {

        Set<String> openedSession = openedSessionContextSet(context);

        // open a new reactive session and store it in the vertx duplicated context
        // the context was marked as "lazy" which means that the session will be eventually closed
        openedSession.add(persistenceUnitName);

        Mutiny.SessionFactory sessionFactory = sessionFactories.getValue(persistenceUnitName);
        MutinySessionImpl session = (MutinySessionImpl) sessionFactory.createSession();

        Key<Mutiny.Session> sessionKey = sessionKeys.getValue(persistenceUnitName);
        context.putLocal(sessionKey, session);

        return session;
    }

    private Set<String> openedSessionContextSet(Context context) {
        // This will keep track of all on-demand opened sessions
        Set<String> onDemandSessionsCreated = context.getLocal(SESSIONS_ON_DEMAND_OPENED_KEY);
        if (onDemandSessionsCreated == null) {
            onDemandSessionsCreated = new HashSet<>();
            context.putLocal(SESSIONS_ON_DEMAND_OPENED_KEY, onDemandSessionsCreated);
        }
        return onDemandSessionsCreated;
    }

    private Uni<Void> closeAndRemoveSession(Context context, Optional<SessionWithKey> openSession) {
        return openSession.map((SessionWithKey s) -> s.session.close()
                .eventually(() -> context.removeLocal(s.key)))
                .orElse(Uni.createFrom().voidItem());
    }

    private Key<Mutiny.Session> createSessionFactoryAndStoreKey(String persistenceUnitName) {
        Mutiny.SessionFactory value = createSessionFactory(persistenceUnitName);
        Implementor implementor = (Implementor) ClientProxy.unwrap(value);
        return new BaseKey<>(Mutiny.Session.class, implementor.getUuid());
    }

    private Mutiny.SessionFactory createSessionFactory(String persistenceunitname) {
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
