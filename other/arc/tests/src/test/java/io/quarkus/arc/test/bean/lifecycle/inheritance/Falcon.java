package io.quarkus.arc.test.bean.lifecycle.inheritance;

import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.Dependent;

// extends Bird class, overrides pre-destroy and post construct with new variant
@Dependent
public class Falcon extends Bird {
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
    public void init() {
        initCalled.incrementAndGet();
    }

    @Override
    public void destroy() {
        destroyCalled.incrementAndGet();
    }

    public void ping() {
    }
}
