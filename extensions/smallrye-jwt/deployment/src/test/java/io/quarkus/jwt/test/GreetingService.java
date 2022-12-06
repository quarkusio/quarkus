package io.quarkus.jwt.test;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String greet() {
        return "hello";
    }
}
