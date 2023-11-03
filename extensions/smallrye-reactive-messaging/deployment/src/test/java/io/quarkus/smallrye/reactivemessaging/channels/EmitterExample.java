package io.quarkus.smallrye.reactivemessaging.channels;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class EmitterExample {

    @Inject
    @Channel("sink")
    Emitter<String> emitter;

    private final List<String> list = new CopyOnWriteArrayList<>();

    public void run() {
        emitter.send("a");
        emitter.send("b");
        emitter.send("c");
        emitter.complete();
    }

    @Incoming("sink")
    public void consume(String s) {
        list.add(s);
    }

    public List<String> list() {
        return list;
    }

}
