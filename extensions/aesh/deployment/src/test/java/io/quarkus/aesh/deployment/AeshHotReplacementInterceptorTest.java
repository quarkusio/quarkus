package io.quarkus.aesh.deployment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.aesh.runtime.devmode.AeshHotReplacementInterceptor;

/**
 * Tests for the dev-mode hot replacement interceptor.
 * These are plain unit tests (no Quarkus container needed) since the interceptor
 * uses only static state.
 */
public class AeshHotReplacementInterceptorTest {

    @AfterEach
    void cleanup() {
        AeshHotReplacementInterceptor.shutdown();
    }

    @Test
    public void testIsActiveBeforeRegister() {
        Assertions.assertThat(AeshHotReplacementInterceptor.isActive()).isFalse();
    }

    @Test
    public void testIsActiveAfterRegister() {
        AeshHotReplacementInterceptor.register(() -> true);
        Assertions.assertThat(AeshHotReplacementInterceptor.isActive()).isTrue();
    }

    @Test
    public void testIsActiveAfterShutdown() {
        AeshHotReplacementInterceptor.register(() -> true);
        AeshHotReplacementInterceptor.shutdown();
        Assertions.assertThat(AeshHotReplacementInterceptor.isActive()).isFalse();
    }

    @Test
    public void testFireAsyncExecutesAction() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean actionCalled = new AtomicBoolean(false);

        AeshHotReplacementInterceptor.register(() -> {
            actionCalled.set(true);
            latch.countDown();
            return true;
        });

        AeshHotReplacementInterceptor.fireAsync();

        Assertions.assertThat(latch.await(5, TimeUnit.SECONDS))
                .as("Action should have been called within 5 seconds")
                .isTrue();
        Assertions.assertThat(actionCalled.get()).isTrue();
    }

    @Test
    public void testFireAsyncWithNoRegistration() {
        // Should not throw when no action is registered
        AeshHotReplacementInterceptor.fireAsync();
    }

    @Test
    public void testFireAsyncAfterShutdown() {
        AeshHotReplacementInterceptor.register(() -> true);
        AeshHotReplacementInterceptor.shutdown();

        // Should not throw after shutdown
        AeshHotReplacementInterceptor.fireAsync();
    }

    @Test
    public void testConcurrentShutdownAndFireAsync() throws Exception {
        // Verify that concurrent shutdown and fireAsync do not cause NPE
        AtomicInteger errors = new AtomicInteger(0);
        for (int i = 0; i < 100; i++) {
            AeshHotReplacementInterceptor.register(() -> true);

            Thread fireThread = new Thread(() -> {
                try {
                    AeshHotReplacementInterceptor.fireAsync();
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
            Thread shutdownThread = new Thread(AeshHotReplacementInterceptor::shutdown);

            fireThread.start();
            shutdownThread.start();
            fireThread.join(1000);
            shutdownThread.join(1000);

            // Ensure clean state for next iteration
            AeshHotReplacementInterceptor.shutdown();
        }

        Assertions.assertThat(errors.get())
                .as("No errors should occur during concurrent shutdown/fireAsync")
                .isEqualTo(0);
    }
}
