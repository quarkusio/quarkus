package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.SkippedExecution;
import io.quarkus.test.QuarkusUnitTest;

public class ConditionalExecutionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class));

    @Test
    public void testExecution() {
        try {
            // Wait until Jobs#doSomething() is executed at least 1x and skipped 1x
            if (IsDisabled.SKIPPED_LATCH.await(10, TimeUnit.SECONDS)) {
                assertEquals(1, Jobs.COUNTER.getCount());
                IsDisabled.DISABLED.set(false);
            } else {
                fail("Job#foo not skipped in 10 seconds!");
            }
            if (!Jobs.COUNTER.await(10, TimeUnit.SECONDS)) {
                fail("Job#foo not executed in 10 seconds!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }

        assertTrue(OtherIsDisabled.TESTED.get());
        assertEquals(0, Jobs.OTHER_COUNT.get());
    }

    static class Jobs {

        static final CountDownLatch COUNTER = new CountDownLatch(1);
        static final AtomicInteger OTHER_COUNT = new AtomicInteger(0);

        @Scheduled(identity = "foo", every = "1s", skipExecutionIf = IsDisabled.class)
        void doSomething() throws InterruptedException {
            COUNTER.countDown();
        }

        @Scheduled(identity = "other-foo", every = "1s", skipExecutionIf = OtherIsDisabled.class)
        void doSomethingElse() throws InterruptedException {
            OTHER_COUNT.incrementAndGet();
        }
    }

    @Singleton
    public static class IsDisabled implements Scheduled.SkipPredicate {

        static final CountDownLatch SKIPPED_LATCH = new CountDownLatch(1);

        static final AtomicBoolean DISABLED = new AtomicBoolean(true);

        @Override
        public boolean test(ScheduledExecution execution) {
            return DISABLED.get();
        }

        void onSkip(@Observes SkippedExecution event) {
            if (event.triggerId.equals("foo")) {

                SKIPPED_LATCH.countDown();
            }
        }

    }

    @Singleton
    public static class OtherIsDisabled implements Scheduled.SkipPredicate {

        static final AtomicBoolean TESTED = new AtomicBoolean(false);

        @Override
        public boolean test(ScheduledExecution execution) {
            TESTED.set(true);
            return true;
        }

    }
}
