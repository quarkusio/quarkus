package io.quarkus.smallrye.reactivemessaging.amqp;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class ProducingBean {

    @Outgoing("source")
    public Publisher<Long> generate() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(100))
                .on().overflow().drop();
    }

}
