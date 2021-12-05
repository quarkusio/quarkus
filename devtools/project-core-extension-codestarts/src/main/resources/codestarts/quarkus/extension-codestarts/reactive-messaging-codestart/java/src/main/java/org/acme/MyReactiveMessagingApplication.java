package org.acme;

import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.reactive.messaging.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.stream.Stream;

@ApplicationScoped
public class MyReactiveMessagingApplication {

    @Inject
    @Channel("source-out")
    Emitter<String> emitter;

    /** Sends message to the source channel, can be used from a JAX-RS resource or any bean of your application **/
    void onStart(@Observes StartupEvent ev) {
        Stream.of("Hello", "with", "SmallRye", "reactive", "message").forEach(string -> emitter.send(string));
    }

    /** Consume the message from the source channel, uppercase it and send it to the uppercase channel **/
    @Incoming("source-in")
    @Outgoing("uppercase-out")
    public Message<String> toUpperCase(Message<String> message) {
        return message.withPayload(message.getPayload().toUpperCase());
    }

    /** Consume the uppercase channel and print the message **/
    @Incoming("uppercase-in")
    public void sink(String word) {
        System.out.println(">> " + word);
    }
}
