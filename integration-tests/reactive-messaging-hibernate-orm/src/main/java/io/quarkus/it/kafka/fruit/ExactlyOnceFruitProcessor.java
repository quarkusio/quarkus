package io.quarkus.it.kafka.fruit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.smallrye.reactivemessaging.kafka.ExactlyOnce;
import io.smallrye.reactive.messaging.kafka.Record;

@ApplicationScoped
public class ExactlyOnceFruitProcessor {

    private final List<String> processed = new CopyOnWriteArrayList<>();

    @Incoming("exactly-once-fruit-in")
    @Outgoing("exactly-once-fruit-out")
    @ExactlyOnce
    @Transactional
    Record<String, String> process(Record<String, String> record) {
        ExactlyOnceFruit fruit = new ExactlyOnceFruit(record.value());
        fruit.persist();
        processed.add(record.value());
        return Record.of(record.key(), "persisted-" + record.value());
    }

    public List<String> getProcessed() {
        return processed;
    }
}
