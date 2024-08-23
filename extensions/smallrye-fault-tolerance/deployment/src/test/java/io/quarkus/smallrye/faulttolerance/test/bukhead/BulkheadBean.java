package io.quarkus.smallrye.faulttolerance.test.bukhead;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

@ApplicationScoped
public class BulkheadBean {
    private AtomicInteger integer = new AtomicInteger();

    @Bulkhead(5)
    public int hello() {
        int i = integer.incrementAndGet();
        try {
            Thread.sleep(100); // artificially generate contention
            return i;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            integer.decrementAndGet();
        }
    }
}
