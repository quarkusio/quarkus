package io.quarkus.smallrye.reactivemessaging.amqp.devmode.nohttp;

import java.time.Duration;
import java.util.concurrent.Flow;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class Producer {
    @Outgoing("source")
    public Flow.Publisher<Long> generate() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(200)).onOverflow().drop().map(i -> i * 1);
    }
}
