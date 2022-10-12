package io.quarkus.smallrye.reactivemessaging.kafka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaConnectorIncomingConfiguration;
import io.smallrye.reactive.messaging.kafka.KafkaConsumer;
import io.smallrye.reactive.messaging.kafka.commit.CheckpointStateStore;
import io.smallrye.reactive.messaging.kafka.commit.KafkaCommitHandler;
import io.smallrye.reactive.messaging.kafka.commit.ProcessingState;
import io.vertx.mutiny.core.Vertx;

public class HibernateOrmStateStore implements CheckpointStateStore {

    public static final String QUARKUS_HIBERNATE_ORM = "quarkus-hibernate-orm";
    private final String consumerGroupId;
    private final SessionFactory sf;
    private final Class<? extends CheckpointEntity> stateType;

    public HibernateOrmStateStore(String consumerGroupId, SessionFactory sf,
            Class<? extends CheckpointEntity> stateType) {
        this.consumerGroupId = consumerGroupId;
        this.sf = sf;
        this.stateType = stateType;
    }

    @ApplicationScoped
    @Identifier(QUARKUS_HIBERNATE_ORM)
    public static class Factory implements CheckpointStateStore.Factory {

        @Inject
        @Any
        Instance<SessionFactory> sessionFactories;

        @Override
        public CheckpointStateStore create(KafkaConnectorIncomingConfiguration config, Vertx vertx,
                KafkaConsumer<?, ?> consumer, Class<?> stateType) {
            String consumerGroupId = (String) consumer.configuration().get(ConsumerConfig.GROUP_ID_CONFIG);
            if (!CheckpointEntity.class.isAssignableFrom(stateType)) {
                throw new IllegalArgumentException("State type needs to extend `CheckpointEntity`");
            }
            String persistenceUnit = config.config().getOptionalValue(KafkaCommitHandler.Strategy.CHECKPOINT + "." +
                    QUARKUS_HIBERNATE_ORM + ".persistence-unit", String.class)
                    .orElse(null);
            SessionFactory sf = persistenceUnit != null
                    ? sessionFactories.select(new PersistenceUnit.PersistenceUnitLiteral(persistenceUnit)).get()
                    : sessionFactories.get();
            return new HibernateOrmStateStore(consumerGroupId, sf, (Class<? extends CheckpointEntity>) stateType);
        }
    }

    @Override
    public Uni<Map<TopicPartition, ProcessingState<?>>> fetchProcessingState(Collection<TopicPartition> partitions) {
        return Uni.createFrom().deferred(() -> {
            Object[] ids = partitions.stream()
                    .map(tp -> new CheckpointEntityId(consumerGroupId, tp))
                    .toArray(Object[]::new);
            return Vertx.currentContext().executeBlocking(Uni.createFrom().item(() -> {
                List<CheckpointEntity> fetched = new ArrayList<>();
                try (Session session = sf.openSession()) {
                    for (Object id : ids) {
                        CheckpointEntity entity = session.find(stateType, id);
                        if (entity != null) {
                            fetched.add(entity);
                        }
                    }
                }
                return fetched.stream().filter(e -> e != null && CheckpointEntity.topicPartition(e) != null)
                        .collect(Collectors.toMap(CheckpointEntity::topicPartition,
                                e -> new ProcessingState<>(e, e.offset)));
            }));
        });
    }

    @Override
    public Uni<Void> persistProcessingState(Map<TopicPartition, ProcessingState<?>> state) {
        return Uni.createFrom().deferred(() -> {
            Object[] entities = state.entrySet().stream()
                    .filter(e -> !ProcessingState.isEmptyOrNull(e.getValue()))
                    .map(e -> CheckpointEntity.from((ProcessingState<? extends CheckpointEntity>) e.getValue(),
                            new CheckpointEntityId(consumerGroupId, e.getKey())))
                    .toArray();
            return Vertx.currentContext().executeBlocking(Uni.createFrom().emitter(e -> {
                Transaction tx = null;
                try (Session session = sf.openSession()) {
                    tx = session.beginTransaction();
                    for (Object entity : entities) {
                        session.merge(entity);
                    }
                    session.flush();
                    tx.commit();
                    e.complete(null);
                } catch (Throwable t) {
                    if (tx != null) {
                        tx.rollback();
                    }
                    e.fail(t);
                }
            }));
        });
    }

}
