package io.quarkus.scheduler.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.RunOnVirtualThread;

public class VirtualThreadClassAnnotationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(Job.class));

    @Test
    void runsOnVirtualThread() throws InterruptedException {
        assertThat(Job.EXECUTED.await(5, TimeUnit.SECONDS)).as("scheduled method was invoked").isTrue();
        assertThat(Job.ranOnVirtualThread).as("scheduled method ran on a virtual thread").isTrue();
    }

    @ApplicationScoped
    @RunOnVirtualThread
    static class Job {

        static final CountDownLatch EXECUTED = new CountDownLatch(1);
        static volatile boolean ranOnVirtualThread;

        @Scheduled(every = "1s")
        void tick() {
            ranOnVirtualThread = Thread.currentThread().isVirtual();
            EXECUTED.countDown();
        }
    }
}
