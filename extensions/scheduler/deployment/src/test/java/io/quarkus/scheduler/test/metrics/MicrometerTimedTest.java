package io.quarkus.scheduler.test.metrics;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class MicrometerTimedTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
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

        waitForMeters(registry.find("scheduled.methods").timers(), 1);
        waitForMeters(registry.find("foo").timers(), 1);

        try {
            Timer timer1 = registry.get("scheduled.methods")
                    .tag("method", "everySecond")
                    .tag("class", "io.quarkus.scheduler.test.metrics.MicrometerTimedTest$Jobs")
                    .tag("exception", "none")
                    .timer();
            assertNotNull(timer1);
            //the count is updated after the method is called
            //so we need to use Awaitility as the metric may not be up to date immediately
            Awaitility.await().pollInterval(10, TimeUnit.MILLISECONDS)
                    .atMost(2, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertTrue(timer1.count() > 0, "Count=" + timer1.count()));
        } catch (MeterNotFoundException e) {
            fail(e.getMessage() + "\nFound: " + registry.find("scheduled.methods").meters().stream()
                    .map(Meter::getId).map(Object::toString).collect(Collectors.joining("\n\t- ")));
        }

        try {
            Timer timer2 = registry.get("foo")
                    .tag("method", "anotherEverySecond")
                    .tag("class", "io.quarkus.scheduler.test.metrics.MicrometerTimedTest$Jobs")
                    .tag("exception", "none")
                    .timer();
            assertNotNull(timer2);
            Awaitility.await().pollInterval(10, TimeUnit.MILLISECONDS)
                    .atMost(2, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertTrue(timer2.count() > 0, "Count=" + timer2.count()));
        } catch (MeterNotFoundException e) {
            fail(e.getMessage() + "\nFound: " + registry.find("foo").meters().stream()
                    .map(Meter::getId).map(Object::toString).collect(Collectors.joining("\n\t- ")));
        }
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

    public static <T> void waitForMeters(Collection<T> collection, int count) throws InterruptedException {
        int i = 0;
        do {
            Thread.sleep(10);
        } while (collection.size() < count && i++ < 5);
    }

}
