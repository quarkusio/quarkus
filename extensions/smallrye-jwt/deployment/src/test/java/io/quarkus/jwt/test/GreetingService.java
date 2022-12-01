package io.quarkus.jwt.test;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String greet() {
        return "hello";
    }
}
