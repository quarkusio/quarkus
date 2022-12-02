package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.interceptor.Interceptor;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class PausedMethodExpressionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PausedMethodExpressionTest.Jobs.class)
                    .addAsResource(new StringAsset("scheduler.identity=myIdentity"),
                            "application.properties"));

    private static final String IDENTITY = "{scheduler.identity}";

    @Test
    public void testPause() throws InterruptedException {
        assertFalse(Jobs.LATCH.await(3, TimeUnit.SECONDS));
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(1);

        @Scheduled(every = "1s", identity = IDENTITY)
        void countDownSecond() {
            LATCH.countDown();
        }

        void pause(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE - 1) StartupEvent event, Scheduler scheduler) {
            // Pause the job before the scheduler starts
            scheduler.pause(IDENTITY);
        }
    }
}
