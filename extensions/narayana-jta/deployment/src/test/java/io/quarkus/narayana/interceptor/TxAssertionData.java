package io.quarkus.narayana.interceptor;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TxAssertionData {
    private AtomicInteger commitNumber = new AtomicInteger();
    private AtomicInteger rollbackNumber = new AtomicInteger();

    public void reset() {
        commitNumber.set(0);
        rollbackNumber.set(0);
    }

    public int addCommit() {
        return commitNumber.incrementAndGet();
    }

    public int addRollback() {
        return rollbackNumber.incrementAndGet();
    }

    public int getCommit() {
        return commitNumber.get();
    }

    public int getRollback() {
        return rollbackNumber.get();
    }
}
