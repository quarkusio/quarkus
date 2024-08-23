package io.quarkus.it.kafka;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.commit.CheckpointMetadata;
import io.smallrye.reactive.messaging.keyed.Keyed;
import io.smallrye.reactive.messaging.keyed.KeyedMulti;

@ApplicationScoped
public class KafkaReceivers {

    private final List<Person> people = new CopyOnWriteArrayList<>();
    private final List<Fruit> fruits = new CopyOnWriteArrayList<>();
    private final List<Record<Pet, Person>> pets = new CopyOnWriteArrayList<>();
    private Map<String, String> dataWithMetadata = new ConcurrentHashMap<>();
    private List<String> dataForKeyed = new CopyOnWriteArrayList<>();

    static class PeopleState {
        public String names;
    }

    @Incoming("people-in")
    public CompletionStage<Void> consume(Message<Person> msg) {
        CheckpointMetadata<PeopleState> store = CheckpointMetadata.fromMessage(msg);
        Person person = msg.getPayload();
        store.transform(new PeopleState(), c -> {
            if (c.names == null || c.names.length() == 0) {
                c.names = person.getName();
            } else {
                c.names = c.names + ";" + person.getName();
            }
            return c;
        });
        people.add(person);
        return msg.ack();
    }

    @Incoming("fruits-in")
    public void consume(Fruit fruit) {
        fruits.add(fruit);
    }

    @Incoming("pets-in")
    public void consume(Record<Pet, Person> pet) {
        pets.add(pet);
    }

    public List<Person> getPeople() {
        return people;
    }

    public List<Fruit> getFruits() {
        return fruits;
    }

    public List<Record<Pet, Person>> getPets() {
        return pets;
    }

    @Incoming("data-with-metadata-in")
    public void consume(String data, IncomingKafkaRecordMetadata<String, String> metadata,
            Optional<IncomingKafkaRecordMetadata<String, String>> metadataAsOptional) {
        if (metadataAsOptional.isEmpty()) {
            throw new IllegalArgumentException("Expected incoming kafka metadata");
        }
        dataWithMetadata.put(data, metadata.getKey());
    }

    public Map<String, String> getDataWithMetadata() {
        return dataWithMetadata;
    }

    @Incoming("data-for-keyed-in")
    @Outgoing("data-for-keyed-intermediary")
    public Multi<String> processFromKafka(KeyedMulti<String, String> data) {
        return data.map(s -> data.key() + "-" + s);
    }

    @Incoming("data-for-keyed-intermediary")
    @Outgoing("data-for-keyed-sink")
    public Multi<String> processIntermediary(@Keyed(MyPayloadExtractor.class) KeyedMulti<String, String> data) {
        return data.map(s -> data.key() + "-" + s);
    }

    @Incoming("data-for-keyed-sink")
    public void consumeDataForKeyed(String s) {
        dataForKeyed.add(s);
    }

    public List<String> getDataForKeyed() {
        return dataForKeyed;
    }
}
