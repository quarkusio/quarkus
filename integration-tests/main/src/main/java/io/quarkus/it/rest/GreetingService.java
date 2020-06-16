package io.quarkus.it.rest;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {
    public String greet(String greeting) {
        return "Hello " + greeting;
    }
}
