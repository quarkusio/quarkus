package io.quarkus.it.rabbitmq;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class PeopleProducer {
    @Outgoing("people-out")
    public Multi<Person> generatePeople() {
        return Uni.createFrom().nullItem()
                .onItem().delayIt().by(Duration.ofSeconds(3))
                .onItem().transformToMulti(n -> Multi.createFrom()
                        .items(new Person("bob"),
                                new Person("alice"),
                                new Person("tom"),
                                new Person("jerry"),
                                new Person("anna"),
                                new Person("ken")));
    }
}
