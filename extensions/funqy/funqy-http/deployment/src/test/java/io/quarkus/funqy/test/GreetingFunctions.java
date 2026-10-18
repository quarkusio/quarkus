package io.quarkus.funqy.test;

import jakarta.inject.Inject;

import io.quarkus.funqy.Funq;
import io.smallrye.mutiny.Uni;

public class GreetingFunctions {
    @Inject
    GreetingService service;

    @Funq("template")
    public void greetingTemplate(GreetingTemplate template) {
        service.setGreeting(template.getGreeting());
        service.setPunctuation(template.getPunctuation());
    }

    @Funq
    public Greeting greet(String name) {
        String message = service.hello(name);
        Greeting greeting = new Greeting();
        greeting.setMessage(message);
        greeting.setName(name);
        return greeting;
    }

    @Funq
    public Uni<Greeting> greetAsync(String name) {
        String message = service.hello(name);
        Greeting greeting = new Greeting();
        greeting.setMessage(message);
        greeting.setName(name);
        return Uni.createFrom().item(() -> greeting);
    }

}
