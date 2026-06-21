package io.quarkus.gcp.function.test.service;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GreetingService {

    @ConfigProperty(name = "greeting.name", defaultValue = "World")
    String name;

    public String hello() {
        return "Hello " + name + "!";
    }
}
