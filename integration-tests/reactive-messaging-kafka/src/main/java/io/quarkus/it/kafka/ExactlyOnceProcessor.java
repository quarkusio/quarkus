package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.logging.Log;
import io.quarkus.smallrye.reactivemessaging.kafka.ExactlyOnce;
import io.smallrye.reactive.messaging.kafka.Record;

@ApplicationScoped
public class ExactlyOnceProcessor {

    private final List<Integer> processed = new CopyOnWriteArrayList<>();

    @Incoming("exactly-once-in")
    @Outgoing("exactly-once-out")
    @ExactlyOnce
    Record<String, Integer> process(Record<String, Integer> record) {
        processed.add(record.value());
        Log.info("Processing record with key: " + record.key() + " and value: " + record.value());
        return Record.of(record.key(), record.value() + 1);
    }

    public List<Integer> getProcessed() {
        return processed;
    }
}
