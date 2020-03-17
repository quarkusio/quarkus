package io.quarkus.funqy.test;

import javax.inject.Inject;

import io.quarkus.funqy.Funq;

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

}
