package io.quarkus.smallrye.reactivemessaging.channels;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.reactive.messaging.annotations.Broadcast;

@ApplicationScoped
public class EmitterWithBroadcastExample {

    @Inject
    @Channel("sink")
    @Broadcast
    Emitter<String> emitter;

    private List<String> list = new CopyOnWriteArrayList<>();
    private List<String> list2 = new CopyOnWriteArrayList<>();

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

    @Incoming("sink")
    public void consume2(String s) {
        list2.add(s);
    }

    public List<String> list() {
        return list;
    }

    public List<String> list2() {
        return list2;
    }

}
