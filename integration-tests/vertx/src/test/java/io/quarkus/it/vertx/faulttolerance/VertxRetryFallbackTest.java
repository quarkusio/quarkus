package io.quarkus.it.vertx.faulttolerance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.core.Vertx;

@QuarkusTest
public class VertxRetryFallbackTest {
    @Inject
    Vertx vertx;

    @Inject
    NonblockingFaultTolerantService service;

    @BeforeEach
    public void setUp() {
        NonblockingFaultTolerantService.invocationThreads.clear();
    }

    @Test
    public void nonblockingRetryFallback() {
        AtomicReference<Object> result = new AtomicReference<>(null);

        vertx.runOnContext(() -> {
            service.hello().subscribe().with(result::set, result::set);
        });

        await().atMost(5, TimeUnit.SECONDS).until(() -> result.get() != null);

        assertThat(result.get()).isEqualTo("Hello fallback!");

        // 1 initial invocation + 10 retries + 1 fallback
        assertThat(NonblockingFaultTolerantService.invocationThreads).hasSize(12);
        assertThat(NonblockingFaultTolerantService.invocationThreads).allSatisfy(thread -> {
            assertThat(thread).contains("vert.x-eventloop");
        });
        assertThat(NonblockingFaultTolerantService.invocationThreads)
                .containsOnly(NonblockingFaultTolerantService.invocationThreads.peek());
    }
}
