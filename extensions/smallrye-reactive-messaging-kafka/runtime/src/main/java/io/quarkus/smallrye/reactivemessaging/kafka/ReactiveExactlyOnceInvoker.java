package io.quarkus.smallrye.reactivemessaging.kafka;

import java.util.concurrent.CompletionStage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordBatchMetadata;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.transactions.KafkaTransactions;
import io.smallrye.reactive.messaging.kafka.transactions.TransactionalEmitter;

public abstract class ReactiveExactlyOnceInvoker extends ExactlyOnceInvoker {

    protected ReactiveExactlyOnceInvoker(String outgoingChannel) {
        super(outgoingChannel);
    }

    @Override
    protected Object invokeBeanInTransaction(KafkaTransactions<Object> tx, Object[] beanArgs,
            IncomingKafkaRecordMetadata<?, ?> recordMeta, IncomingKafkaRecordBatchMetadata<?, ?> batchMeta) {
        if (batchMeta != null) {
            return tx.withTransaction(batchMeta, emitter -> handleResult(invokeBean(beanArgs), emitter));
        } else if (recordMeta != null) {
            return tx.withTransaction(recordMeta, emitter -> handleResult(invokeBean(beanArgs), emitter));
        } else {
            throw new IllegalStateException("No Kafka record metadata found on incoming message");
        }
    }

    private Uni<Void> handleResult(Object result, TransactionalEmitter<Object> emitter) {
        if (result == null) {
            return Uni.createFrom().voidItem();
        }
        if (result instanceof Uni<?> uni) {
            return uni.onItem().invoke(item -> sendResult(item, emitter))
                    .replaceWithVoid();
        }
        if (result instanceof CompletionStage<?> cs) {
            return Uni.createFrom().completionStage(cs)
                    .onItem().invoke(item -> sendResult(item, emitter))
                    .replaceWithVoid();
        }
        if (result instanceof Multi<?> multi) {
            return multi.onItem().invoke(emitter::send)
                    .onItem().ignoreAsUni();
        }
        sendResult(result, emitter);
        return Uni.createFrom().voidItem();
    }
}
