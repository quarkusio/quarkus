package io.quarkus.aesh.deployment;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String greet(String name) {
        return "Hello " + name + " from CDI!";
    }
}
