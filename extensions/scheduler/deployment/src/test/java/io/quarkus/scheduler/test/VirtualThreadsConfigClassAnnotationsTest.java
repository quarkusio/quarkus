package io.quarkus.scheduler.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;

public class VirtualThreadsConfigClassAnnotationsTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Recorder.class, BlockingBean.class, NonBlockingBean.class,
                    VirtualBean.class, StaticBlockingBean.class))
            .overrideConfigKey("quarkus.scheduler.blocking.signatures.execution.mode", "virtual-thread");

    @Test
    public void testClassLevelAnnotations() throws InterruptedException {
        for (Map.Entry<String, CountDownLatch> e : Recorder.LATCHES.entrySet()) {
            assertTrue(e.getValue().await(5, TimeUnit.SECONDS), "Job not executed: " + e.getKey());
        }
        // class-level @Blocking keeps jobs on a worker thread
        assertThat(Recorder.VIRTUAL.get("blockingClass")).isFalse();
        // a method-level annotation takes precedence over the class-level one
        assertThat(Recorder.VIRTUAL.get("blockingClassVirtualMethod")).isTrue();
        // class-level @NonBlocking keeps jobs on the event loop
        assertThat(Recorder.VIRTUAL.get("nonBlockingClass")).isFalse();
        // class-level @RunOnVirtualThread
        assertThat(Recorder.VIRTUAL.get("virtualClass")).isTrue();
        // class-level annotations also apply to static scheduled methods
        assertThat(Recorder.VIRTUAL.get("staticBlockingClass")).isFalse();
    }

    static class Recorder {

        static final Map<String, CountDownLatch> LATCHES = Map.of(
                "blockingClass", new CountDownLatch(1),
                "blockingClassVirtualMethod", new CountDownLatch(1),
                "nonBlockingClass", new CountDownLatch(1),
                "virtualClass", new CountDownLatch(1),
                "staticBlockingClass", new CountDownLatch(1));

        static final Map<String, Boolean> VIRTUAL = new ConcurrentHashMap<>();

        static void record(String name) {
            VIRTUAL.putIfAbsent(name, Thread.currentThread().isVirtual());
            LATCHES.get(name).countDown();
        }
    }

    @Blocking
    @ApplicationScoped
    static class BlockingBean {

        @Scheduled(every = "0.2s")
        void job() {
            Recorder.record("blockingClass");
        }

        @RunOnVirtualThread
        @Scheduled(every = "0.2s")
        void virtualJob() {
            Recorder.record("blockingClassVirtualMethod");
        }
    }

    @NonBlocking
    @ApplicationScoped
    static class NonBlockingBean {

        @Scheduled(every = "0.2s")
        void job() {
            Recorder.record("nonBlockingClass");
        }
    }

    @RunOnVirtualThread
    @ApplicationScoped
    static class VirtualBean {

        @Scheduled(every = "0.2s")
        void job() {
            Recorder.record("virtualClass");
        }
    }

    @Blocking
    static class StaticBlockingBean {

        @Scheduled(every = "0.2s")
        static void job() {
            Recorder.record("staticBlockingClass");
        }
    }

}
