package io.quarkus.smallrye.reactivemessaging.blocking.beans;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.reactive.messaging.annotations.Blocking;

@ApplicationScoped
public class IncomingCustomTwoBlockingBean {
    private List<String> list = new CopyOnWriteArrayList<>();
    private List<String> threads = new CopyOnWriteArrayList<>();

    @Incoming("in")
    @Blocking("another-pool")
    public void consume(String s) {
        threads.add(Thread.currentThread().getName());
        list.add(s);
    }

    public List<String> list() {
        return list;
    }

    public List<String> threads() {
        return threads;
    }
}
