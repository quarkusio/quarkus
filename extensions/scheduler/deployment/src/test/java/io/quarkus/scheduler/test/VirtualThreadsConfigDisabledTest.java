package io.quarkus.scheduler.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;

public class VirtualThreadsConfigDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Jobs.class))
            .overrideConfigKey("quarkus.scheduler.blocking.signatures.execution.mode", "virtual-thread")
            .overrideConfigKey("quarkus.virtual-threads.enabled", "false");

    @Test
    public void testFallbackToWorkerThreads() throws InterruptedException {
        for (Map.Entry<String, CountDownLatch> e : Jobs.LATCHES.entrySet()) {
            assertTrue(e.getValue().await(5, TimeUnit.SECONDS), "Job not executed: " + e.getKey());
        }
        // with virtual threads disabled, nothing runs on a virtual thread
        assertThat(Jobs.VIRTUAL).containsOnlyKeys("default", "blocking", "nonBlocking", "virtual");
        assertThat(Jobs.VIRTUAL.values()).containsOnly(false);
    }

    static class Jobs {

        static final Map<String, CountDownLatch> LATCHES = Map.of(
                "default", new CountDownLatch(1),
                "blocking", new CountDownLatch(1),
                "nonBlocking", new CountDownLatch(1),
                "virtual", new CountDownLatch(1));

        static final Map<String, Boolean> VIRTUAL = new ConcurrentHashMap<>();

        static void record(String name) {
            VIRTUAL.putIfAbsent(name, Thread.currentThread().isVirtual());
            LATCHES.get(name).countDown();
        }

        @Scheduled(every = "0.2s")
        void defaultJob() {
            record("default");
        }

        @Blocking
        @Scheduled(every = "0.2s")
        void blockingJob() {
            record("blocking");
        }

        @NonBlocking
        @Scheduled(every = "0.2s")
        void nonBlockingJob() {
            record("nonBlocking");
        }

        @RunOnVirtualThread
        @Scheduled(every = "0.2s")
        void virtualJob() {
            record("virtual");
        }
    }

}
