package io.quarkus.scheduler.test.staticmethod;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class ScheduledStaticMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class, AbstractJobs.class, InterfaceJobs.class));

    @Test
    public void testSimpleScheduledJobs() throws InterruptedException {
        assertTrue(Jobs.LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(AbstractJobs.LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(InterfaceJobs.LATCH.await(5, TimeUnit.SECONDS));
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(1);

        @Scheduled(every = "1s")
        static void everySecond() {
            LATCH.countDown();
        }

    }

    static abstract class AbstractJobs {

        static final CountDownLatch LATCH = new CountDownLatch(1);

        @Scheduled(every = "1s")
        static void everySecond() {
            LATCH.countDown();
        }

    }

    interface InterfaceJobs {

        CountDownLatch LATCH = new CountDownLatch(1);

        @Scheduled(every = "1s")
        static void everySecond() {
            LATCH.countDown();
        }

    }

}
