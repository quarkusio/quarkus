package io.quarkus.it.pulsar;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import examples.HelloRequest;

@ApplicationScoped
public class PulsarReceivers {

    private final List<String> fruits = new CopyOnWriteArrayList<>();

    @Incoming("fruits-in")
    public void consume(HelloRequest fruit) {
        System.out.println(fruit.getName());
        fruits.add(fruit.getName());
    }

    public List<String> getFruits() {
        return fruits;
    }

}
