package io.quarkus.websockets.next.test.requestcontext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestScopedBean {

    static final CountDownLatch DESTROYED_LATCH = new CountDownLatch(3);
    static final AtomicInteger COUNTER = new AtomicInteger();

    private int id;

    @PostConstruct
    void init() {
        id = COUNTER.incrementAndGet();
    }

    public String appendId(String message) {
        return message + ":" + id;
    }

    @PreDestroy
    void destroy() {
        DESTROYED_LATCH.countDown();
    }

}
