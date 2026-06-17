package io.quarkus.it.aesh.websocket;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String greet(String name) {
        return "Hello " + name + " from service!";
    }
}
