package io.quarkus.it.vertx;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class EventBusConsumer {

    @ConsumeEvent("pets")
    public String sayHi(Pet pet) {
        return "Hello " + pet.getName() + " (" + pet.getKind() + ")";
    }

    @ConsumeEvent("persons")
    public String name(String name) {
        return "Hello " + name;
    }

}
