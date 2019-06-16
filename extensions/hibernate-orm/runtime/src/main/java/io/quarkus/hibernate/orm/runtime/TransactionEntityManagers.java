package io.quarkus.hibernate.orm.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import io.quarkus.hibernate.orm.runtime.entitymanager.TransactionScopedEntityManager;

public class TransactionEntityManagers {

    @Inject
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Inject
    TransactionManager transactionManager;

    @Inject
    JPAConfig jpaConfig;

    @Inject
    Instance<RequestScopedEntityManagerHolder> requestScopedEntityManagers;

    private final ConcurrentMap<String, TransactionScopedEntityManager> managers;

    public TransactionEntityManagers() {
        this.managers = new ConcurrentHashMap<>();
    }

    public EntityManager getEntityManager(String unitName) {
        TransactionScopedEntityManager entityManager = managers.get(unitName);
        if (entityManager != null) {
            return entityManager;
        }
        return managers.computeIfAbsent(unitName, (un) -> new TransactionScopedEntityManager(
                transactionManager, transactionSynchronizationRegistry, jpaConfig.getEntityManagerFactory(un), un,
                requestScopedEntityManagers));
    }

}
