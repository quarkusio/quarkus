package io.quarkus.context.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.junit.jupiter.api.Assertions;

@ApplicationScoped
public class TransactionalBean {

    @Inject
    TransactionManager tm;

    @Transactional(value = TxType.REQUIRES_NEW)
    public void doInTx() {
        Assertions.assertEquals(0, ContextEntity.count());

        ContextEntity entity = new ContextEntity();
        entity.name = "Stef";
        entity.persist();
    }
}
