package io.quarkus.it.amqp;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.vertx.core.json.Json;

@ApplicationScoped
public class PeopleProducer {

    @Outgoing("people-out")
    public Multi<String> generatePeople() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(100))
                .on().overflow().drop()
                .flatMap(x -> Multi.createFrom().items(
                        new Person("bob"),
                        new Person("alice"),
                        new Person("tom"),
                        new Person("jerry"),
                        new Person("anna"),
                        new Person("ken")).map(Json::encode));
    }
}
