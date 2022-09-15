package io.quarkus.smallrye.reactivemessaging.channels;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Emitter;

@ApplicationScoped
public class DeprecatedEmitterExample {

    @Inject
    @Channel("sink")
    Emitter<String> emitter;

    private List<String> list = new CopyOnWriteArrayList<>();

    public void run() {
        emitter.send("a").send("b").send("c").complete();
    }

    @Incoming("sink")
    public void consume(String s) {
        list.add(s);
    }

    public List<String> list() {
        return list;
    }

}
