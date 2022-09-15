package io.quarkus.arc.test.unused;

import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;

@Bravo // Intercepted by Bravo, injected in Alpha!
@Singleton
public class Counter {

    private AtomicInteger counter = new AtomicInteger();

    public int incrementAndGet() {
        return counter.incrementAndGet();
    }

    public void reset() {
        counter.set(0);
    }

    public int get() {
        return counter.get();
    }

}
