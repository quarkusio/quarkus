package io.quarkus.context.test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.Assertions;
import org.reactivestreams.Publisher;

import io.quarkus.hibernate.orm.panache.Panache;
import io.reactivex.Flowable;
import io.reactivex.Single;

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

    @Transactional(value = TxType.REQUIRES_NEW)
    public Single<String> doInTxSingle() {
        Assertions.assertEquals(0, ContextEntity.count());

        ContextEntity entity = new ContextEntity();
        entity.name = "Stef";
        entity.persist();

        return Single.just("OK");
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public Flowable<String> doInTxPublisher() {
        Assertions.assertEquals(0, ContextEntity.count());

        ContextEntity entity = new ContextEntity();
        entity.name = "Stef";
        entity.persist();

        return Flowable.fromArray("OK");
    }
}
