package io.quarkus.smallrye.reactivemessaging.kafka;

import java.util.Arrays;

import jakarta.enterprise.inject.spi.CDI;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.ChannelRegistry;
import io.smallrye.reactive.messaging.Invoker;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordBatchMetadata;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.transactions.KafkaTransactions;
import io.smallrye.reactive.messaging.kafka.transactions.TransactionalEmitter;

public abstract class ExactlyOnceInvoker implements Invoker {

    private static final int SYNTHETIC_PARAM_COUNT = 2;

    private final KafkaTransactions<Object> kafkaTransactions;

    @SuppressWarnings("unchecked")
    protected ExactlyOnceInvoker(String outgoingChannel) {
        ChannelRegistry registry = CDI.current().select(ChannelRegistry.class).get();
        this.kafkaTransactions = registry.getEmitter(outgoingChannel, KafkaTransactions.class);
    }

    protected abstract Object invokeBean(Object... args);

    @Override
    public Object invoke(Object... args) {
        IncomingKafkaRecordMetadata<?, ?> recordMeta = (IncomingKafkaRecordMetadata<?, ?>) args[args.length - 2];
        IncomingKafkaRecordBatchMetadata<?, ?> batchMeta = (IncomingKafkaRecordBatchMetadata<?, ?>) args[args.length - 1];

        Object[] beanArgs = Arrays.copyOf(args, args.length - SYNTHETIC_PARAM_COUNT);

        return invokeBeanInTransaction(kafkaTransactions, beanArgs, recordMeta, batchMeta);
    }

    protected Object invokeBeanInTransaction(KafkaTransactions<Object> tx, Object[] beanArgs,
            IncomingKafkaRecordMetadata<?, ?> recordMeta, IncomingKafkaRecordBatchMetadata<?, ?> batchMeta) {
        if (batchMeta != null) {
            tx.withTransactionAndAwait(batchMeta, emitter -> {
                sendResult(invokeBean(beanArgs), emitter);
                return Uni.createFrom().voidItem();
            });
        } else if (recordMeta != null) {
            tx.withTransactionAndAwait(recordMeta, emitter -> {
                sendResult(invokeBean(beanArgs), emitter);
                return Uni.createFrom().voidItem();
            });
        } else {
            throw new IllegalStateException("No Kafka record metadata found on incoming message");
        }
        return null;
    }

    protected void sendResult(Object result, TransactionalEmitter<Object> emitter) {
        if (result == null) {
            return;
        }
        if (result instanceof Iterable<?> items) {
            for (Object item : items) {
                emitter.send(item);
            }
        } else if (result instanceof Multi<?> multi) {
            for (Object item : multi.subscribe().asIterable()) {
                emitter.send(item);
            }
        } else {
            emitter.send(result);
        }
    }

}
