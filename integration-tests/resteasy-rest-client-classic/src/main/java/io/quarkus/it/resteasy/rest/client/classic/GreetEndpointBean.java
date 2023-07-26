package io.quarkus.it.resteasy.rest.client.classic;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("")
@ApplicationScoped
public class GreetEndpointBean implements GreetEndpoint {
    private final String greeting;
    private final AtomicInteger number = new AtomicInteger(0);

    GreetEndpointBean(
            @ConfigProperty(name = "greeting", defaultValue = "Hello") String greeting) {
        this.greeting = greeting;
    }

    @Override
    public Greet greet(String name) {
        return new Greet(greeting, name, number.incrementAndGet());
    }
}
