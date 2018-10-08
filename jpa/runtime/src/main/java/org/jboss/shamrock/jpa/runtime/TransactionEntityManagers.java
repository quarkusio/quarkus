package org.jboss.shamrock.jpa.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

@RequestScoped
public class TransactionEntityManagers {

    @Inject
    TransactionSynchronizationRegistry tsr;

    @Inject
    TransactionManager tm;

    @Inject
    JPAConfig jpaConfig;

    private final Map<String, TransactionScopedEntityManager> managers;

    public TransactionEntityManagers() {
        this.managers = new HashMap<>();
    }

    EntityManager getEntityManager(String unitName) {
        return managers.computeIfAbsent(unitName,
                un -> new TransactionScopedEntityManager(tm, tsr, jpaConfig.getEntityManagerFactory(un)));
    }

    @PreDestroy
    void destroy() {
        for (TransactionScopedEntityManager manager : managers.values()) {
            manager.requestDone();
        }
    }

}
