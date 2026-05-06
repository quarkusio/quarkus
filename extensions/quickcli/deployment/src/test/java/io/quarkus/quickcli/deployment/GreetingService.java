package io.quarkus.quickcli.deployment;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String greet(String greeting) {
        return greeting + " from service!";
    }
}
