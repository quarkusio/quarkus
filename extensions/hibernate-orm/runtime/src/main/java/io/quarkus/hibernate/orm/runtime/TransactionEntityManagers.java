package io.quarkus.hibernate.orm.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.entitymanager.TransactionScopedEntityManager;

@ApplicationScoped
public class TransactionEntityManagers {

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
                getTransactionManager(), getTransactionSynchronizationRegistry(), jpaConfig.getEntityManagerFactory(un), un,
                requestScopedEntityManagers));
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
