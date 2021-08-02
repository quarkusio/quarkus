package io.quarkus.scheduler.test.metrics;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class MicrometerTimedTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("quarkus.scheduler.metrics.enabled=true"),
                            "application.properties"));

    @Inject
    MeterRegistry registry;

    @BeforeAll
    static void addSimpleRegistry() {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    @Test
    void testTimedMethod() throws InterruptedException {
        assertTrue(Jobs.latch01.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.latch02.await(5, TimeUnit.SECONDS));
        Timer timer1 = registry.get("scheduled.methods")
                .tag("method", "everySecond")
                .tag("class", "io.quarkus.scheduler.test.metrics.MicrometerTimedTest$Jobs")
                .tag("exception", "none")
                .timer();
        assertNotNull(timer1);
        assertTrue(timer1.count() > 0);
        Timer timer2 = registry.get("foo")
                .tag("method", "anotherEverySecond")
                .tag("class", "io.quarkus.scheduler.test.metrics.MicrometerTimedTest$Jobs")
                .tag("exception", "none")
                .timer();
        assertNotNull(timer2);
        assertTrue(timer2.count() > 0);
    }

    static class Jobs {

        static final CountDownLatch latch01 = new CountDownLatch(1);
        static final CountDownLatch latch02 = new CountDownLatch(1);

        @Scheduled(every = "1s")
        void everySecond() {
            latch01.countDown();
        }

        @Timed("foo") // Extension should not override this annotation 
        @Scheduled(every = "1s")
        void anotherEverySecond() {
            latch02.countDown();
        }

    }

}
