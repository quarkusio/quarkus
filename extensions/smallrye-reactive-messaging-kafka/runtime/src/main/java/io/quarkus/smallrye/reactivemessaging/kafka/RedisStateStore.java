package io.quarkus.smallrye.reactivemessaging.kafka;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.reactive.messaging.kafka.KafkaConnectorIncomingConfiguration;
import io.smallrye.reactive.messaging.kafka.KafkaConsumer;
import io.smallrye.reactive.messaging.kafka.commit.CheckpointStateStore;
import io.smallrye.reactive.messaging.kafka.commit.KafkaCommitHandler;
import io.smallrye.reactive.messaging.kafka.commit.ProcessingState;
import io.smallrye.reactive.messaging.kafka.commit.ProcessingStateCodec;
import io.smallrye.reactive.messaging.kafka.commit.VertxJsonProcessingStateCodec;
import io.smallrye.reactive.messaging.providers.helpers.CDIUtils;
import io.vertx.mutiny.core.Vertx;

public class RedisStateStore implements CheckpointStateStore {

    public static final String REDIS_CHECKPOINT_NAME = "quarkus-redis";

    private final ReactiveRedisDataSource redis;
    private final String consumerGroupId;
    private final ProcessingStateCodec stateCodec;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public RedisStateStore(ReactiveRedisDataSource redis, String consumerGroupId, ProcessingStateCodec stateCodec) {
        this.redis = redis;
        this.consumerGroupId = consumerGroupId;
        this.stateCodec = stateCodec;
    }

    @ApplicationScoped
    @Identifier(REDIS_CHECKPOINT_NAME)
    public static class Factory implements CheckpointStateStore.Factory {

        @Inject
        @Any
        Instance<ReactiveRedisDataSource> redisDataSource;

        @Inject
        Instance<ProcessingStateCodec.Factory> stateCodecFactory;

        @Override
        public CheckpointStateStore create(KafkaConnectorIncomingConfiguration config, Vertx vertx,
                KafkaConsumer<?, ?> consumer, Class<?> stateType) {
            String consumerGroupId = (String) consumer.configuration().get(ConsumerConfig.GROUP_ID_CONFIG);
            String clientName = config.config().getOptionalValue(KafkaCommitHandler.Strategy.CHECKPOINT + "." +
                    REDIS_CHECKPOINT_NAME + ".client-name", String.class)
                    .orElse(null);
            ReactiveRedisDataSource rds = clientName != null
                    ? redisDataSource.select(RedisClientName.Literal.of(clientName)).get()
                    : redisDataSource.get();
            ProcessingStateCodec stateCodec = CDIUtils.getInstanceById(stateCodecFactory, config.getChannel(), () -> {
                if (stateCodecFactory.isUnsatisfied()) {
                    return VertxJsonProcessingStateCodec.FACTORY;
                } else {
                    return stateCodecFactory.get();
                }
            }).create(stateType);
            return new RedisStateStore(rds, consumerGroupId, stateCodec);
        }
    }

    @Override
    public void close() {
        closed.set(true);
    }

    @Override
    public Uni<Map<TopicPartition, ProcessingState<?>>> fetchProcessingState(Collection<TopicPartition> partitions) {
        if (partitions.isEmpty() || closed.get()) {
            return Uni.createFrom().item(Collections.emptyMap());
        }
        List<Tuple2<TopicPartition, String>> tps = partitions.stream()
                .map(tp -> Tuple2.of(tp, getKey(tp)))
                .collect(Collectors.toList());
        return redis.value(byte[].class).mget(tps.stream().map(Tuple2::getItem2).toArray(String[]::new))
                .map(response -> response.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .collect(Collectors.toMap(e -> getTpFromKey(e.getKey()),
                                e -> ProcessingState.getOrEmpty(stateCodec.decode(e.getValue())))));
    }

    private String getKey(TopicPartition partition) {
        return consumerGroupId + ":" + partition.topic() + ":" + partition.partition();
    }

    private TopicPartition getTpFromKey(String key) {
        String[] parts = key.split(":");
        return new TopicPartition(parts[1], Integer.parseInt(parts[2]));
    }

    @Override
    public Uni<Void> persistProcessingState(Map<TopicPartition, ProcessingState<?>> states) {
        if (states.isEmpty() || closed.get()) {
            return Uni.createFrom().voidItem();
        }
        String[] keys = states.keySet().stream().map(this::getKey).toArray(String[]::new);
        return redis.withTransaction(r -> r.value(byte[].class).mget(keys), (current, r) -> {
            Map<String, byte[]> map = states.entrySet().stream().filter(toPersist -> {
                String key = getKey(toPersist.getKey());
                ProcessingState<?> newState = toPersist.getValue();
                if (!current.containsKey(key)) {
                    return true;
                }
                ProcessingState<?> currentState = stateCodec.decode(current.get(key));
                return ProcessingState.isEmptyOrNull(currentState) ||
                        (!ProcessingState.isEmptyOrNull(newState) && newState.getOffset() >= currentState.getOffset());
            }).collect(Collectors.toMap(e -> getKey(e.getKey()), e -> stateCodec.encode(e.getValue())));
            if (map.isEmpty()) {
                return Uni.createFrom().voidItem();
            } else {
                return r.value(byte[].class).mset(map);
            }
        }, keys).replaceWithVoid();
    }

}
