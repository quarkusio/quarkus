package io.quarkus.smallrye.faulttolerance.test.bukhead;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

@ApplicationScoped
public class BulkheadBean {
    private AtomicInteger integer = new AtomicInteger();

    @Bulkhead(5)
    public int bulkhead() {
        int i = integer.incrementAndGet();
        try {
            Thread.sleep(10);//artificially generates contention
            return i;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            integer.decrementAndGet();
        }
    }
}
