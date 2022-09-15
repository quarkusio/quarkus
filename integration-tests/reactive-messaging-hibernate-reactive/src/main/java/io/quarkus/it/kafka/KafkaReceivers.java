package io.quarkus.it.kafka;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Blocking;

@ApplicationScoped
public class KafkaReceivers {

    @Inject
    Mutiny.SessionFactory sf;

    @Incoming("fruits-in")
    @Outgoing("fruits-persisted")
    public Uni<Message<Fruit>> persistFruit(Message<Fruit> fruit) {
        assert VertxContext.isOnDuplicatedContext();
        Fruit payload = fruit.getPayload();
        return sf.withTransaction(session -> {
            return session.persist(payload).chain(x -> session.fetch(payload).map(p -> {
                // ContextLocals is only callable on duplicated context
                ContextLocals.put("fruit-id", p.id);
                return fruit.withPayload(payload);
            }));
        });
    }

    @Blocking
    @Incoming("fruits-persisted")
    public Uni<Void> consumeFruit(Message<Fruit> fruit) {
        assert VertxContext.isOnDuplicatedContext();
        assert Objects.equals(ContextLocals.get("fruit-id").get(), fruit.getPayload().id);
        return Uni.createFrom().completionStage(fruit.ack());
    }

    public Uni<List<Fruit>> getFruits() {
        return Fruit.listAll();
    }

}
