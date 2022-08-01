package io.quarkus.arc.test.bean.lifecycle.inheritance;

import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;

@Dependent
public class Eagle extends Bird {

    private static final AtomicInteger initCalled = new AtomicInteger();
    private static final AtomicInteger destroyCalled = new AtomicInteger();

    public static AtomicInteger getInitCalled() {
        return initCalled;
    }

    public static AtomicInteger getDestroyCalled() {
        return destroyCalled;
    }

    public static void reset() {
        initCalled.set(0);
        destroyCalled.set(0);
    }

    @Override
    @PostConstruct
    public void init() {
        initCalled.incrementAndGet();
    }

    @Override
    @PreDestroy
    public void destroy() {
        destroyCalled.incrementAndGet();
    }

    public void ping() {
    }
}
