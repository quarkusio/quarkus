package io.quarkus.smallrye.faulttolerance.test.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BulkheadConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(BulkheadConfigBean.class))
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.BulkheadConfigBean/value\".bulkhead.value",
                    "1")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.BulkheadConfigBean/waitingTaskQueue\".bulkhead.waiting-task-queue",
                    "1");

    @Inject
    private BulkheadConfigBean bean;

    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void value() throws Exception {
        CompletableFuture<?> barrier = new CompletableFuture<>();

        executor.submit(() -> bean.value(barrier));
        Thread.sleep(500);
        assertThatThrownBy(() -> bean.value(null)).isExactlyInstanceOf(BulkheadException.class);

        barrier.complete(null);
    }

    @Test
    public void waitingTaskQueue() throws Exception {
        CompletableFuture<?> barrier1 = new CompletableFuture<>();
        CompletableFuture<?> barrier2 = new CompletableFuture<>();

        executor.submit(() -> bean.waitingTaskQueue(barrier1));
        executor.submit(() -> bean.waitingTaskQueue(barrier2));
        Thread.sleep(500);
        assertThatThrownBy(() -> bean.waitingTaskQueue(null).get())
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(BulkheadException.class);

        barrier1.complete(null);
        barrier2.complete(null);
    }
}
