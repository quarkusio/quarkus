package io.quarkus.smallrye.faulttolerance.test.retry.beforeretry;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;

@Dependent
public class MyDependency {
    private static final AtomicInteger counter = new AtomicInteger();

    public final int id = counter.incrementAndGet();
}
