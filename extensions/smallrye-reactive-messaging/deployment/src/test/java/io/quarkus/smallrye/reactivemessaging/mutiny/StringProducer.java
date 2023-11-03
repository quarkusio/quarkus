package io.quarkus.smallrye.reactivemessaging.mutiny;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class StringProducer {

    public static final String STRING_STREAM = "string-producer";
    public static final String ANOTHER_STRING_STREAM = "another-string-producer";

    @Outgoing(STRING_STREAM)
    public Multi<String> produceStrings() {
        return Multi.createFrom().items("hello", "world", "from", "smallrye", "reactive", "messaging");
    }

    @Outgoing(ANOTHER_STRING_STREAM)
    public Multi<String> produceDifferentStrings() {
        return Multi.createFrom().items("some", "other", "text");
    }
}
