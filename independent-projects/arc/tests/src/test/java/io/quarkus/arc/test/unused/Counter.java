package io.quarkus.arc.test.unused;

import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Singleton;

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
