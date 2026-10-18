package io.quarkus.it.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PeopleManager {

    @Inject
    @Channel("people-out")
    Emitter<byte[]> emitter;

    private final Logger log = Logger.getLogger(PeopleManager.class);

    private final List<String> list = new CopyOnWriteArrayList<>();

    @Incoming("people-in")
    public void consume(byte[] raw) {
        list.add(new String(raw));
    }

    public List<String> getPeople() {
        log.info("Returning people " + list);
        return list;
    }

    public void seedPeople() {
        Stream
                .of("bob",
                        "alice",
                        "tom",
                        "jerry",
                        "anna",
                        "ken")
                .forEach(s -> emitter.send(s.getBytes(StandardCharsets.UTF_8)));
    }
}
