package io.quarkus.smallrye.reactivemessaging.kafka;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.transaction.TransactionManager;

import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordBatchMetadata;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.transactions.KafkaTransactions;

/**
 * Wraps the Kafka transaction inside a JTA transaction so that both commit/rollback together.
 * The bean's {@code @Transactional(REQUIRED)} joins the outer JTA transaction instead of managing its own.
 */
public abstract class TransactionalExactlyOnceInvoker extends ExactlyOnceInvoker {

    private volatile TransactionManager transactionManager;

    protected TransactionalExactlyOnceInvoker(String outgoingChannel) {
        super(outgoingChannel);
    }

    @Override
    protected Object invokeBeanInTransaction(KafkaTransactions<Object> tx, Object[] beanArgs,
            IncomingKafkaRecordMetadata<?, ?> recordMeta, IncomingKafkaRecordBatchMetadata<?, ?> batchMeta) {
        TransactionManager tm = getTransactionManager();
        try {
            tm.begin();
            try {
                super.invokeBeanInTransaction(tx, beanArgs, recordMeta, batchMeta);
                tm.commit();
            } catch (Exception e) {
                tm.rollback();
                throw e;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private TransactionManager getTransactionManager() {
        if (transactionManager == null) {
            transactionManager = CDI.current().select(TransactionManager.class).get();
        }
        return transactionManager;
    }
}
