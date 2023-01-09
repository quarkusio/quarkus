package io.quarkus.smallrye.reactivemessaging.amqp.devmode.nohttp;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class Producer {
    @Outgoing("source")
    public Publisher<Long> generate() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(200))
                .onOverflow().drop()
                .map(i -> i * 1);
    }
}
