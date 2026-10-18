package io.quarkus.smallrye.reactivemessaging.blocking.beans;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.common.annotation.Blocking;

@ApplicationScoped
public class BeanReturningPayloadsUsingSmallRyeBlocking {
    private final AtomicInteger count = new AtomicInteger();
    private final List<String> threads = new CopyOnWriteArrayList<>();

    @Blocking
    @Outgoing("infinite-producer-payload")
    public int create() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        threads.add(Thread.currentThread().getName());
        return count.incrementAndGet();
    }

    public List<String> threads() {
        return threads;
    }
}
