package io.quarkus.it.kafka;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.commit.CheckpointMetadata;

@ApplicationScoped
public class KafkaReceivers {

    private final List<Person> people = new CopyOnWriteArrayList<>();

    private final List<Pet> pets = new CopyOnWriteArrayList<>();

    @Incoming("fruits-in")
    @Outgoing("fruits-persisted")
    @WithTransaction
    public Uni<Message<Fruit>> persistFruit(Message<Fruit> fruit) {
        assert VertxContext.isOnDuplicatedContext();
        Fruit payload = fruit.getPayload();
        payload.name = "fruit-" + payload.name;
        return payload.persist().map(x -> {
            // ContextLocals is only callable on duplicated context;
            ContextLocals.put("fruit-id", payload.id);
            return fruit;
        });
    }

    @Blocking
    @Incoming("fruits-persisted")
    public void consumeFruit(Fruit fruit) {
        assert VertxContext.isOnDuplicatedContext();
        assert Objects.equals(ContextLocals.get("fruit-id").get(), fruit.id);
    }

    @Incoming("pets-in")
    public void consumePet(Pet pet) {
        pets.add(pet);
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

    @WithSession
    public Uni<List<Fruit>> getFruits() {
        return Fruit.listAll();
    }

    public List<Person> getPeople() {
        return people;
    }

    @WithSession
    public Uni<List<Pet>> getPets() {
        return Pet.listAll();
    }

    public List<Pet> getConsumedPets() {
        return pets;
    }
}
