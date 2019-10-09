package io.quarkus.smallrye.reactivemessaging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Emitter;

@ApplicationScoped
public class StreamEmitter {

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
