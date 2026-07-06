package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.reactive.messaging.kafka.Record;

@ApplicationScoped
public class ExactlyOnceConsumer {

    private final List<Integer> results = new CopyOnWriteArrayList<>();

    @Incoming("exactly-once-result")
    void consume(Record<String, Integer> record) {
        results.add(record.value());
    }

    public List<Integer> getResults() {
        return results;
    }
}
