package io.quarkus.hibernate.orm.deployment.test;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;

import io.quarkus.narayana.jta.runtime.test.TestTransactionCallback;

public class HibernateTestTransactionCallback implements TestTransactionCallback {
    @Override
    public void postBegin() {

    }

    @Override
    public void preRollback() {
        for (EntityManager i : CDI.current().select(EntityManager.class)) {
            i.flush();
        }

    }
}
