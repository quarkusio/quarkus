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
import io.smallrye.mutiny.Uni;

public class VirtualThreadsConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Jobs.class))
            .overrideConfigKey("quarkus.scheduler.virtual-threads", "true");

    @Test
    public void testExecutionModels() throws InterruptedException {
        for (Map.Entry<String, CountDownLatch> e : Jobs.LATCHES.entrySet()) {
            assertTrue(e.getValue().await(5, TimeUnit.SECONDS), "Job not executed: " + e.getKey());
        }
        // blocking jobs without an explicit execution model annotation default to a virtual thread
        assertThat(Jobs.VIRTUAL.get("default")).isTrue();
        assertThat(Jobs.VIRTUAL.get("virtual")).isTrue();
        // an explicit @Blocking annotation keeps the job on a worker thread
        assertThat(Jobs.VIRTUAL.get("blocking")).isFalse();
        // non-blocking jobs keep running on the event loop
        assertThat(Jobs.VIRTUAL.get("nonBlocking")).isFalse();
        assertThat(Jobs.VIRTUAL.get("uni")).isFalse();
    }

    static class Jobs {

        static final Map<String, CountDownLatch> LATCHES = Map.of(
                "default", new CountDownLatch(1),
                "blocking", new CountDownLatch(1),
                "nonBlocking", new CountDownLatch(1),
                "uni", new CountDownLatch(1),
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

        @Scheduled(every = "0.2s")
        Uni<Void> uniJob() {
            record("uni");
            return Uni.createFrom().voidItem();
        }

        @RunOnVirtualThread
        @Scheduled(every = "0.2s")
        void virtualJob() {
            record("virtual");
        }
    }

}
