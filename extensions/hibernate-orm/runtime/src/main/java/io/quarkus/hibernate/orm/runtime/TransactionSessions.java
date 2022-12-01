package io.quarkus.hibernate.orm.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.session.TransactionScopedSession;

@ApplicationScoped
public class TransactionSessions {

    @Inject
    JPAConfig jpaConfig;

    @Inject
    Instance<RequestScopedSessionHolder> requestScopedSession;

    private final ConcurrentMap<String, TransactionScopedSession> sessions;

    public TransactionSessions() {
        this.sessions = new ConcurrentHashMap<>();
    }

    public Session getSession(String unitName) {
        TransactionScopedSession session = sessions.get(unitName);
        if (session != null) {
            return session;
        }
        return sessions.computeIfAbsent(unitName, (un) -> new TransactionScopedSession(
                getTransactionManager(), getTransactionSynchronizationRegistry(),
                jpaConfig.getEntityManagerFactory(un).unwrap(SessionFactory.class), un,
                requestScopedSession));
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
