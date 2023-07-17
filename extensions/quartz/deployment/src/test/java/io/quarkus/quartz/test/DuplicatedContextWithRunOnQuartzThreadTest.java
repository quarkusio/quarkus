package io.quarkus.quartz.test;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Verifies that the non-blocking @Scheduled method are called on a duplicated context, but blocking methods are not
 * because of the {@code quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread} option.
 */
public class DuplicatedContextWithRunOnQuartzThreadTest {
    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyScheduledClass.class))
            .overrideConfigKey("quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread", "true");

    @Inject
    MyScheduledClass scheduled;

    @Test
    public void testBlocking() {
        await()
                .atMost(Duration.ofSeconds(3))
                .until(() -> scheduled.blockingCalled() > 0);
    }

    @Test
    public void testNonBlocking() {
        await()
                .atMost(Duration.ofSeconds(3))
                .until(() -> scheduled.nonBlockingCalled() > 0);
    }

    public static class MyScheduledClass {

        private final AtomicInteger blockingCalled = new AtomicInteger();
        private final AtomicInteger nonBlockingCalled = new AtomicInteger();

        @Scheduled(every = "1m")
        public void blocking() {
            Context context = Vertx.currentContext();
            Assertions.assertNull(context);
            blockingCalled.incrementAndGet();
        }

        @Scheduled(every = "1m")
        public Uni<Void> nonblocking() {
            Context context = Vertx.currentContext();
            Assertions.assertNotNull(context);
            Assertions.assertTrue(VertxContext.isDuplicatedContext(context));
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());

            nonBlockingCalled.incrementAndGet();
            return Uni.createFrom().voidItem();
        }

        public int blockingCalled() {
            return blockingCalled.get();
        }

        public int nonBlockingCalled() {
            return nonBlockingCalled.get();
        }
    }
}
