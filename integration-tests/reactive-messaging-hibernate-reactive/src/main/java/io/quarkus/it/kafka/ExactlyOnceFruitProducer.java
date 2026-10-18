package io.quarkus.it.kafka;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;

@ApplicationScoped
public class ExactlyOnceFruitProducer {

    @Outgoing("exactly-once-fruit-source")
    Multi<Record<String, String>> produce() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(100))
                .onOverflow().drop()
                .map(tick -> Record.of("fruit-" + tick, "fruit-" + tick))
                .select().first(4);
    }
}
