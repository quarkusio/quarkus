package io.quarkus.it.amqp;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class PeopleManager {

    private List<Person> list = new CopyOnWriteArrayList<>();

    @Incoming("people-in")
    public void consume(String json) {
        Person person = new JsonObject(json).mapTo(Person.class);
        list.add(person);
    }

    public List<Person> getPeople() {
        return list;
    }

}
