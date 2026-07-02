package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.logging.Log;
import io.quarkus.smallrye.reactivemessaging.kafka.ExactlyOnce;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.Record;

@ApplicationScoped
public class ExactlyOnceUniProcessor {

    private final List<Integer> processed = new CopyOnWriteArrayList<>();

    @Incoming("exactly-once-uni-in")
    @Outgoing("exactly-once-uni-out")
    @ExactlyOnce
    Uni<Record<String, Integer>> process(Record<String, Integer> record) {
        Log.info("Uni processing record with key: " + record.key() + " and value: " + record.value());
        return Uni.createFrom().item(record)
                .onItem().transform(r -> {
                    processed.add(r.value());
                    return Record.of(r.key(), r.value() + 1);
                });
    }

    public List<Integer> getProcessed() {
        return processed;
    }
}
