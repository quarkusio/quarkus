package io.quarkus.it.kafka;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.commit.CheckpointMetadata;

@ApplicationScoped
public class KafkaReceivers {

    private final List<Person> people = new CopyOnWriteArrayList<>();

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
    @ActivateRequestContext
    public Uni<Void> consumeFruit(Message<Fruit> fruit) {
        assert VertxContext.isOnDuplicatedContext();
        Fruit payload = fruit.getPayload();
        assert Objects.equals(ContextLocals.get("fruit-id").get(), payload.id);
        return Panache.withTransaction(() -> {
            payload.name = "fruit-" + payload.name;
            return payload.persist().chain(() -> Uni.createFrom().completionStage(fruit.ack()));
        });
    }

    @Incoming("people-in")
    public CompletionStage<Void> consume(Message<Person> msg) {
        CheckpointMetadata<PeopleState> store = CheckpointMetadata.fromMessage(msg);
        Person person = msg.getPayload();
        store.transform(new PeopleState(), c -> {
            if (c.names == null) {
                c.names = person.getName();
            } else {
                c.names = c.names + ";" + person.getName();
            }
            return c;
        });
        people.add(person);
        return msg.ack();
    }

    public Uni<List<Fruit>> getFruits() {
        return Fruit.listAll();
    }

    public List<Person> getPeople() {
        return people;
    }

}
