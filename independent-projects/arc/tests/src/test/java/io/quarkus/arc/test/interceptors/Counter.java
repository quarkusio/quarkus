package io.quarkus.arc.test.interceptors;

import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Singleton;

@Singleton
public class Counter {

    private AtomicInteger counter = new AtomicInteger();

    int incrementAndGet() {
        return counter.incrementAndGet();
    }

    void reset() {
        counter.set(0);
    }

    int get() {
        return counter.get();
    }

}
