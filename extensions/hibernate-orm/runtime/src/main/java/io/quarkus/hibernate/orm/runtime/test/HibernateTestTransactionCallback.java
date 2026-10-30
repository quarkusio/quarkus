package io.quarkus.hibernate.orm.runtime.test;

import org.hibernate.Session;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.TransactionSessions;
import io.quarkus.hibernate.orm.runtime.session.TransactionScopedSession;
import io.quarkus.narayana.jta.runtime.test.TestTransactionCallback;

public class HibernateTestTransactionCallback implements TestTransactionCallback {
    @Override
    public void postBegin() {

    }

    @Override
    public void preRollback() {
        for (TransactionScopedSession transactionScopedSession : Arc.container().instance(TransactionSessions.class).get()
                .getSessions()) {
            Session session = transactionScopedSession.getSessionIfExists();
            if (session != null) {
                session.flush();
            }
        }

    }
}
