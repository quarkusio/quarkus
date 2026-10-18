package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.smallrye.reactivemessaging.kafka.ExactlyOnce;
import io.smallrye.reactive.messaging.kafka.Record;

@ApplicationScoped
public class ExactlyOnceListProcessor {

    private final List<Integer> processed = new CopyOnWriteArrayList<>();

    @Incoming("exactly-once-list-in")
    @Outgoing("exactly-once-list-out")
    @ExactlyOnce
    List<Record<String, Integer>> process(Record<String, Integer> record) {
        processed.add(record.value());
        return List.of(
                Record.of(record.key(), record.value() * 10),
                Record.of(record.key(), record.value() * 10 + 1));
    }

    public List<Integer> getProcessed() {
        return processed;
    }
}
