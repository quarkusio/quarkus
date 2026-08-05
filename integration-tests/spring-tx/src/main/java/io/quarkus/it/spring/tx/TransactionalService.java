package io.quarkus.it.spring.tx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ApplicationScoped
public class TransactionalService {

    @Inject
    TransactionManager tm;

    @Transactional
    public boolean defaultTx() throws Exception {
        return tm.getTransaction() != null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean requiresNewTx() throws Exception {
        return tm.getTransaction() != null;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean supportsTx() throws Exception {
        return tm.getTransaction() != null;
    }

    @Transactional(propagation = Propagation.NEVER)
    public boolean neverTx() throws Exception {
        return tm.getTransaction() != null;
    }

    @Transactional(rollbackFor = IllegalArgumentException.class)
    public void rollbackFor() {
        throw new IllegalArgumentException("expected");
    }
}
