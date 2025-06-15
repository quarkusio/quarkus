package io.quarkus.scheduler.test.tracing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class OpenTelemetryTracingTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(Jobs.class)
            .addAsResource(new StringAsset("quarkus.scheduler.tracing.enabled=true"), "application.properties"));

    @Test
    void testWithSpan() throws InterruptedException {
        assertTrue(Jobs.latch.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.nonBlockingLatch.await(5, TimeUnit.SECONDS));

        // assert that different span ids were used
        assertTrue(Jobs.spanIds.stream().collect(Collectors.toSet()).size() >= 2);
        assertTrue(Jobs.nonBlockingSpanIds.stream().collect(Collectors.toSet()).size() >= 2);
    }

    static class Jobs {

        static final CountDownLatch latch = new CountDownLatch(2);
        static final CountDownLatch nonBlockingLatch = new CountDownLatch(2);

        static final List<String> spanIds = new CopyOnWriteArrayList<>();
        static final List<String> nonBlockingSpanIds = new CopyOnWriteArrayList<>();

        @Scheduled(every = "1s")
        void everySecond() {
            spanIds.add(Span.current().getSpanContext().getSpanId());
            latch.countDown();
        }

        @Scheduled(every = "1s")
        Uni<Void> everySecondNonBlocking() {
            return Uni.createFrom().item(() -> {
                nonBlockingSpanIds.add(Span.current().getSpanContext().getSpanId());
                nonBlockingLatch.countDown();
                return true;
            }).replaceWithVoid();
        }

    }

}
