package io.quarkus.it.rabbitmq;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class PeopleManager {

    private final Logger log = Logger.getLogger(PeopleManager.class);

    private final List<Person> list = new CopyOnWriteArrayList<>();

    @Incoming("people-in")
    public void consume(JsonObject message) {
        list.add(message.mapTo(Person.class));
    }

    public List<Person> getPeople() {
        log.info("Returning people " + list);
        return list;
    }
}
