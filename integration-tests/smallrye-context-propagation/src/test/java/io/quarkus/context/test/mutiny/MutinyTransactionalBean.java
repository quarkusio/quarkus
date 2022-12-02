package io.quarkus.context.test.mutiny;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.junit.jupiter.api.Assertions;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MutinyTransactionalBean {

    @Inject
    TransactionManager tm;

    @Transactional(value = TxType.REQUIRES_NEW)
    public void doInTx() {
        Assertions.assertEquals(0, Person.count());

        Person entity = new Person();
        entity.name = "Stef";
        entity.persist();
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public Uni<String> doInTxUni() {
        Assertions.assertEquals(0, Person.count());

        Person entity = new Person();
        entity.name = "Stef";
        entity.persist();

        return Uni.createFrom().item("OK");
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public Multi<String> doInTxMulti() {
        Assertions.assertEquals(0, Person.count());

        Person entity = new Person();
        entity.name = "Stef";
        entity.persist();

        return Multi.createFrom().items("OK");
    }
}
