package io.quarkus.smallrye.reactivemessaging.devmode;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.reactive.messaging.annotations.Broadcast;

@ApplicationScoped
public class MyProcessor {

    @Incoming("input")
    @Outgoing("processed")
    @Broadcast
    public String process(String input) {
        return input.toUpperCase();
    }
}
