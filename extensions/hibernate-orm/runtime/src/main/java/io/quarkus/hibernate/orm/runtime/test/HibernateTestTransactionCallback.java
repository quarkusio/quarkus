package io.quarkus.hibernate.orm.runtime.test;

import jakarta.enterprise.inject.Any;
import jakarta.persistence.EntityManager;

import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.runtime.test.TestTransactionCallback;

public class HibernateTestTransactionCallback implements TestTransactionCallback {
    @Override
    public void postBegin() {

    }

    @Override
    public void preRollback() {
        for (var handle : Arc.container().select(EntityManager.class, Any.Literal.INSTANCE).handles()) {
            if (handle.getBean().isActive()) {
                handle.get().flush();
            }
        }

    }
}
