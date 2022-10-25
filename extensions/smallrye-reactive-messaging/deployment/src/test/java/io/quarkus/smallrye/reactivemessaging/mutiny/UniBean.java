package io.quarkus.smallrye.reactivemessaging.mutiny;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class UniBean {
    private static final String OUT_STREAM = "exclamated-stream";

    final List<String> strings = Collections.synchronizedList(new ArrayList<>());

    @Incoming(StringProducer.ANOTHER_STRING_STREAM)
    @Outgoing(OUT_STREAM)
    public Uni<String> transform(String input) {
        return Uni.createFrom().item(input).map(this::exclamate);
    }

    @Incoming(OUT_STREAM)
    public void collect(String input) {
        strings.add(input);
    }

    private String exclamate(String in) {
        return in + "!";
    }

    public List<String> getStrings() {
        return strings;
    }
}
