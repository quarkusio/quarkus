package io.quarkus.gcp.function.test.service;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {
    public String hello() {
        return "Hello World!";
    }
}
