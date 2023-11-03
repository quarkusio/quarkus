package io.quarkus.context.test;

import java.util.concurrent.CountDownLatch;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {

    public static volatile int DESTROY_INVOKED = 0;
    public static volatile CountDownLatch LATCH;

    public String callMe() {
        return "Hello " + System.identityHashCode(this);
    }

    @PreDestroy
    public void destroy() {
        DESTROY_INVOKED++;
        // bean is also used in tests where there is no latch
        if (LATCH != null) {
            LATCH.countDown();
        }
    }

    public static void initState() {
        LATCH = new CountDownLatch(2);
        DESTROY_INVOKED = 0;
    }
}
