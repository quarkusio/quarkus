package io.quarkus.hibernate.orm.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import io.quarkus.hibernate.orm.runtime.entitymanager.TransactionScopedEntityManager;

public class TransactionEntityManagers {

    @Inject
    TransactionSynchronizationRegistry tsr;

    @Inject
    TransactionManager tm;

    @Inject
    JPAConfig jpaConfig;

    @Inject
    Instance<RequestScopedEntityManagerHolder> requestScopedEntityManagers;

    private final Map<String, TransactionScopedEntityManager> managers;

    public TransactionEntityManagers() {
        this.managers = new HashMap<>();
    }

    public EntityManager getEntityManager(String unitName) {
        return managers.computeIfAbsent(unitName,
                un -> new TransactionScopedEntityManager(tm, tsr, jpaConfig.getEntityManagerFactory(un), unitName,
                        requestScopedEntityManagers));
    }

}
