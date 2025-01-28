package io.quarkus.hibernate.orm.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.session.TransactionScopedSession;
import io.quarkus.hibernate.orm.runtime.session.TransactionScopedStatelessSession;

@ApplicationScoped
public class TransactionSessions {

    @Inject
    JPAConfig jpaConfig;

    @Inject
    Instance<RequestScopedSessionHolder> requestScopedSession;

    @Inject
    Instance<RequestScopedStatelessSessionHolder> requestScopedStatelessSession;

    private final ConcurrentMap<String, TransactionScopedSession> sessions;
    private final ConcurrentMap<String, TransactionScopedStatelessSession> staleSessions;

    public TransactionSessions() {
        this.sessions = new ConcurrentHashMap<>();
        this.staleSessions = new ConcurrentHashMap<>();
    }

    public Session getSession(String unitName) {
        TransactionScopedSession session = sessions.get(unitName);
        if (session != null) {
            return session;
        }
        return sessions.computeIfAbsent(unitName, (un) -> new TransactionScopedSession(
                getTransactionManager(), getTransactionSynchronizationRegistry(),
                jpaConfig.getEntityManagerFactory(un).unwrap(SessionFactory.class), un,
                jpaConfig.getRequestScopedSessionEnabled(), requestScopedSession));
    }

    public StatelessSession getStatelessSession(String unitName) {
        TransactionScopedStatelessSession session = staleSessions.get(unitName);
        if (session != null) {
            return session;
        }
        return staleSessions.computeIfAbsent(unitName, (un) -> new TransactionScopedStatelessSession(
                getTransactionManager(), getTransactionSynchronizationRegistry(),
                jpaConfig.getEntityManagerFactory(un).unwrap(SessionFactory.class), un,
                jpaConfig.getRequestScopedSessionEnabled(), requestScopedStatelessSession));
    }

    private TransactionManager getTransactionManager() {
        return Arc.container()
                .instance(TransactionManager.class).get();
    }

    private TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return Arc.container()
                .instance(TransactionSynchronizationRegistry.class).get();
    }

}
