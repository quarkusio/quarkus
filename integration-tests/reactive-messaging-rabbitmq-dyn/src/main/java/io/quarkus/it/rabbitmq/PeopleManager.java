package io.quarkus.it.rabbitmq;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class PeopleManager {

    private final List<Person> list = new CopyOnWriteArrayList<>();

    @Incoming("people-in")
    public void consume(JsonObject message) {
        list.add(message.mapTo(Person.class));
    }

    public List<Person> getPeople() {
        return list;
    }
}
