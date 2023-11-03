package io.quarkus.narayana.jta;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.TransactionScoped;

@TransactionScoped
public class TransactionScopedBean {

    private static final AtomicInteger initializedCount = new AtomicInteger(0);
    private static final AtomicInteger destroyedCount = new AtomicInteger(0);

    private int value = 0;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    static void resetCounters() {
        initializedCount.set(0);
        destroyedCount.set(0);
    }

    static int getInitializedCount() {
        return initializedCount.get();
    }

    static int getPreDestroyCount() {
        return destroyedCount.get();
    }

    @PostConstruct
    void postConstruct() {
        initializedCount.incrementAndGet();
    }

    @PreDestroy
    void preDestroy() {
        destroyedCount.incrementAndGet();
    }

}
