package io.quarkus.quartz.test;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.assertj.core.data.TemporalUnitWithinOffset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.quarkus.test.QuarkusUnitTest;

public class FireTimeTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("quarkus.scheduler.overdue-grace-period=2H"),
                            "application.properties"));

    @Inject
    Scheduler scheduler;

    @Test
    public void testExecution() throws InterruptedException {
        Jobs.firstLatch.await();

        assertFireTimes();

        Jobs.secondLatch.await();

        assertFireTimes();
    }

    private void assertFireTimes() {
        assertThat(Jobs.previousFireTime.get()).isCloseTo(Jobs.capturedTime.get(),
                new TemporalUnitWithinOffset(100, ChronoUnit.MILLIS));
        assertThat(Jobs.nextFireTime.get()).isCloseTo(Jobs.capturedTime.get().plusSeconds(5),
                new TemporalUnitWithinOffset(100, ChronoUnit.MILLIS));
    }

    static class Jobs {

        static AtomicReference<Instant> nextFireTime = new AtomicReference<>(null);
        static AtomicReference<Instant> previousFireTime = new AtomicReference<>(null);
        static AtomicReference<Instant> capturedTime = new AtomicReference<>(null);
        static AtomicInteger count = new AtomicInteger(0);
        static CountDownLatch firstLatch = new CountDownLatch(1);
        static CountDownLatch secondLatch = new CountDownLatch(2);

        @Inject
        Scheduler scheduler;

        @Scheduled(identity = "test", every = "5s")
        void someJob() {
            Trigger trigger = scheduler.getScheduledJob("test");
            nextFireTime.set(trigger.getNextFireTime());
            previousFireTime.set(trigger.getPreviousFireTime());
            capturedTime.set(Instant.now());
            int newCount = count.incrementAndGet();
            if (newCount == 1) {
                secondLatch.countDown();
                firstLatch.countDown();
            } else if (newCount == 2) {
                secondLatch.countDown();
            }
        }
    }
}
