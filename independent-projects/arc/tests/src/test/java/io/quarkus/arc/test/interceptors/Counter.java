package io.quarkus.arc.test.interceptors;

import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Singleton;

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
