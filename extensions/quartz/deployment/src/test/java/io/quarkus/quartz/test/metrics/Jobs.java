package io.quarkus.quartz.test.metrics;

import java.util.concurrent.CountDownLatch;

import org.eclipse.microprofile.metrics.annotation.Timed;

import io.quarkus.scheduler.Scheduled;

public class Jobs {

    static final CountDownLatch latch01 = new CountDownLatch(1);
    static final CountDownLatch latch02 = new CountDownLatch(1);

    @Scheduled(every = "1s")
    void everySecond() {
        latch01.countDown();
    }

    @Timed(name = "foo") // Extension should not override this annotation 
    @Scheduled(every = "1s")
    void anotherEverySecond() {
        latch02.countDown();
    }

}