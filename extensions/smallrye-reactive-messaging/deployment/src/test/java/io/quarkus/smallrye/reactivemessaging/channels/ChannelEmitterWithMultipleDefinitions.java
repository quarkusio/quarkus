package io.quarkus.smallrye.reactivemessaging.channels;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

import io.smallrye.reactive.messaging.annotations.Broadcast;

@ApplicationScoped
public class ChannelEmitterWithMultipleDefinitions {

    @Inject
    @Channel("sink")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER)
    @Broadcast
    Emitter<String> emitter;

    private Emitter<String> emitterForSink2;

    @Inject
    public void setEmitter(
            @Channel("sink") @Broadcast @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 4) Emitter<String> sink2) {
        this.emitterForSink2 = sink2;
    }

    private final List<String> list = new CopyOnWriteArrayList<>();
    private final List<String> sink1 = new CopyOnWriteArrayList<>();
    private final List<String> sink2 = new CopyOnWriteArrayList<>();

    private final List<String> list2 = new CopyOnWriteArrayList<>();
    private final List<String> sink12 = new CopyOnWriteArrayList<>();
    private final List<String> sink22 = new CopyOnWriteArrayList<>();

    public void run() {
        emitter.send("a");
        emitter.send("b");
        emitter.send("c").toCompletableFuture().join();
        emitter.complete();
        emitterForSink2.send("a2").toCompletableFuture().join();
        emitterForSink2.send("b2");
        emitterForSink2.send("c2");
        emitterForSink2.complete();
    }

    @Incoming("sink")
    public void consume1(String s) {
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

    public List<String> sink11() {
        return sink1;
    }

    public List<String> sink12() {
        return sink12;
    }

    public List<String> sink21() {
        return sink2;
    }

    public List<String> sink22() {
        return sink22;
    }

}
