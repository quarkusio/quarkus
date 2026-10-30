package io.quarkus.hibernate.orm.runtime.test;

import jakarta.enterprise.inject.Any;

import org.hibernate.Session;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.hibernate.orm.runtime.session.TransactionScopedSession;
import io.quarkus.narayana.jta.runtime.test.TestTransactionCallback;

public class HibernateTestTransactionCallback implements TestTransactionCallback {
    @Override
    public void postBegin() {

    }

    @Override
    public void preRollback() {
        for (Session session : Arc.container().select(Session.class, Any.Literal.INSTANCE).listActive()) {
            Session unwrapped = ClientProxy.unwrap(session);
            if (unwrapped instanceof TransactionScopedSession transactionScopedSession) {
                Session delegate = transactionScopedSession.getSessionIfExists();
                if (delegate != null) {
                    delegate.flush();
                }
            }
        }

    }
}
