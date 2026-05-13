package io.quarkus.smallrye.reactivemessaging.kafka;

import jakarta.enterprise.inject.spi.CDI;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordBatchMetadata;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.transactions.KafkaTransactions;

/**
 * Wraps the Kafka transaction inside a Hibernate Reactive transaction so that both commit/rollback together.
 */
public abstract class ReactiveTransactionalExactlyOnceInvoker extends ReactiveExactlyOnceInvoker {

    private volatile Mutiny.SessionFactory sessionFactory;

    protected ReactiveTransactionalExactlyOnceInvoker(String outgoingChannel) {
        super(outgoingChannel);
    }

    @Override
    protected Object invokeBeanInTransaction(KafkaTransactions<Object> tx, Object[] beanArgs,
            IncomingKafkaRecordMetadata<?, ?> recordMeta, IncomingKafkaRecordBatchMetadata<?, ?> batchMeta) {
        return getSessionFactory().withTransaction(
                (session, transaction) -> (Uni<?>) super.invokeBeanInTransaction(tx, beanArgs, recordMeta, batchMeta));
    }

    private Mutiny.SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = CDI.current().select(Mutiny.SessionFactory.class).get();
        }
        return sessionFactory;
    }
}
