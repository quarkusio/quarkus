package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class PausedSchedulerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PausedSchedulerTest.Jobs.class));

    @Inject
    Scheduler scheduler;

    @Test
    public void testSchedulerPauseMethod() throws InterruptedException {
        scheduler.pause();
        assertFalse(scheduler.isRunning());
        assertFalse(Jobs.LATCH.await(3, TimeUnit.SECONDS));
    }

    static class Jobs {
        static final CountDownLatch LATCH = new CountDownLatch(2);

        @Scheduled(every = "1s")
        void countDownSecond() {
            LATCH.countDown();
        }
    }

}
