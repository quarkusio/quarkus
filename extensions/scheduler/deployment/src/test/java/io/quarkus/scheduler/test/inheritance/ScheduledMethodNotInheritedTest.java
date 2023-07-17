package io.quarkus.scheduler.test.inheritance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class ScheduledMethodNotInheritedTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class, MyJobs.class));

    @Test
    public void testExecution() throws InterruptedException {
        assertTrue(Jobs.LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.JOB_CLASSES.stream().allMatch(s -> s.equals(Jobs.class.getName())));
    }

    @Singleton
    static class MyJobs extends Jobs {

    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(2);
        static final List<String> JOB_CLASSES = new CopyOnWriteArrayList<>();

        @Scheduled(every = "1s", identity = "foo")
        void everySecond() {
            JOB_CLASSES.add(getClass().getName());
            LATCH.countDown();
        }
    }
}
