package io.quarkus.context.test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

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
