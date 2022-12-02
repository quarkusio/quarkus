package io.quarkus.smallrye.reactivemessaging.kafka.deployment.dev;

import java.time.Duration;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class PriceGenerator {
    private final Random random = new Random();

    @Outgoing("generated-price")
    public Multi<Integer> generate() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .onOverflow().drop()
                .map(tick -> this.random.nextInt(100));
    }
}
