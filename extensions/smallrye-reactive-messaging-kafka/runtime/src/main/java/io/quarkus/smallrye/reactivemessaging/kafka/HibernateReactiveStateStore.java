package io.quarkus.smallrye.reactivemessaging.kafka;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaConnectorIncomingConfiguration;
import io.smallrye.reactive.messaging.kafka.KafkaConsumer;
import io.smallrye.reactive.messaging.kafka.commit.CheckpointStateStore;
import io.smallrye.reactive.messaging.kafka.commit.ProcessingState;
import io.vertx.core.Context;
import io.vertx.mutiny.core.Vertx;

public class HibernateReactiveStateStore implements CheckpointStateStore {

    private final String consumerGroupId;
    private final Mutiny.SessionFactory sf;
    private final Class<? extends CheckpointEntity> stateType;

    public HibernateReactiveStateStore(String consumerGroupId, Mutiny.SessionFactory sf,
            Class<? extends CheckpointEntity> stateType) {
        this.consumerGroupId = consumerGroupId;
        this.sf = sf;
        this.stateType = stateType;
    }

    @ApplicationScoped
    @Identifier("quarkus-hibernate-reactive")
    public static class Factory implements CheckpointStateStore.Factory {

        @Inject
        Mutiny.SessionFactory sf;

        @Override
        public CheckpointStateStore create(KafkaConnectorIncomingConfiguration config, Vertx vertx,
                KafkaConsumer<?, ?> consumer, Class<?> stateType) {
            String consumerGroupId = (String) consumer.configuration().get(ConsumerConfig.GROUP_ID_CONFIG);
            if (!CheckpointEntity.class.isAssignableFrom(stateType)) {
                throw new IllegalArgumentException("State type needs to extend `CheckpointEntity`");
            }
            return new HibernateReactiveStateStore(consumerGroupId, sf, (Class<? extends CheckpointEntity>) stateType);
        }
    }

    @Override
    public Uni<Map<TopicPartition, ProcessingState<?>>> fetchProcessingState(Collection<TopicPartition> partitions) {
        return Uni.createFrom().<Map<TopicPartition, ProcessingState<?>>> deferred(() -> {
            Object[] ids = partitions.stream()
                    .map(tp -> new CheckpointEntityId(consumerGroupId, tp))
                    .toArray(Object[]::new);
            return sf.withTransaction((s) -> s.find(stateType, ids))
                    .map(fetched -> {
                        if (fetched == null) {
                            return Collections.emptyMap();
                        } else {
                            return fetched.stream()
                                    .filter(e -> e != null && CheckpointEntity.topicPartition(e) != null)
                                    .collect(Collectors.toMap(CheckpointEntity::topicPartition,
                                            e -> new ProcessingState<CheckpointEntity>(e, e.offset)));
                        }
                    });
        }).runSubscriptionOn(HibernateReactiveStateStore::runOnSafeContext);
    }

    @Override
    public Uni<Void> persistProcessingState(Map<TopicPartition, ProcessingState<?>> state) {
        return Uni.createFrom().deferred(() -> {
            Object[] entities = state.entrySet().stream()
                    .filter(e -> !ProcessingState.isEmptyOrNull(e.getValue()))
                    .map(e -> CheckpointEntity.from((ProcessingState<? extends CheckpointEntity>) e.getValue(),
                            new CheckpointEntityId(consumerGroupId, e.getKey())))
                    .toArray();
            return sf.withTransaction(s -> s.mergeAll(entities));
        }).runSubscriptionOn(HibernateReactiveStateStore::runOnSafeContext);
    }

    private static void runOnSafeContext(Runnable r) {
        if (VertxContext.isOnDuplicatedContext()) {
            VertxContextSafetyToggle.setCurrentContextSafe(true);
            r.run();
        } else {
            Context duplicatedContext = VertxContext.createNewDuplicatedContext();
            VertxContextSafetyToggle.setContextSafe(duplicatedContext, true);
            duplicatedContext.runOnContext(x -> r.run());
        }
    }

}
