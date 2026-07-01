package io.quarkus.it.kafka;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;

@ApplicationScoped
public class ExactlyOnceListProducer {

    @Outgoing("exactly-once-list-source")
    Multi<Record<String, Integer>> produce() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(100))
                .onOverflow().drop()
                .map(tick -> Record.of("key-" + tick, tick.intValue()))
                .select().first(4);
    }
}
