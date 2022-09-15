package io.quarkus.smallrye.reactivemessaging.amqp;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ProducingBean {

    @Outgoing("source")
    public Publisher<Long> generate() {
        return Multi.createFrom().range(1, 11)
                .map(Integer::longValue)
                .map(i -> i * 2)
                .onItem()
                .transformToUniAndConcatenate(l -> Uni.createFrom().item(l).onItem().delayIt().by(Duration.ofMillis(10)));
    }

}
