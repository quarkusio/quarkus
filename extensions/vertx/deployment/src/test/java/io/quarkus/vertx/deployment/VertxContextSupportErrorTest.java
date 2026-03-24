package io.quarkus.vertx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

public class VertxContextSupportErrorTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(ErrorTestBean.class));

    @Inject
    ErrorTestBean bean;

    @Test
    public void testSubscribeAndAwaitPropagatesRuntimeException() {
        assertNotNull(bean.runtimeExResult);
        assertInstanceOf(RuntimeException.class, bean.runtimeExResult);
        assertEquals("expected runtime failure", bean.runtimeExResult.getMessage());
    }

    @Test
    public void testSubscribeAndAwaitPropagatesCheckedException() {
        assertNotNull(bean.checkedExResult);
        // Checked exceptions get wrapped in CompletionException by the Uni await mechanism
        assertInstanceOf(CompletionException.class, bean.checkedExResult);
        Throwable cause = bean.checkedExResult.getCause();
        assertInstanceOf(IOException.class, cause);
        assertEquals("expected checked failure", cause.getMessage());
    }

    @Test
    public void testSubscribeAndAwaitPropagatesExceptionThrownInSupplier() {
        assertNotNull(bean.supplierExResult);
        assertInstanceOf(IllegalArgumentException.class, bean.supplierExResult);
        assertEquals("supplier failure", bean.supplierExResult.getMessage());
    }

    @Test
    public void testExecuteBlockingSuccess() throws InterruptedException {
        assertTrue(bean.executeBlockingLatch.await(5, TimeUnit.SECONDS));
        assertEquals("blocking-result", bean.executeBlockingResult.get());
    }

    @Test
    public void testExecuteBlockingRunsOnNonEventLoop() throws InterruptedException {
        assertTrue(bean.executeBlockingLatch.await(5, TimeUnit.SECONDS));
        assertNotNull(bean.executeBlockingThreadName.get());
        // Blocking work should NOT run on an event loop thread
        assertFalse(bean.executeBlockingThreadName.get().contains("vert.x-eventloop"),
                "Expected non-event-loop thread but got: " + bean.executeBlockingThreadName.get());
    }

    @Singleton
    public static class ErrorTestBean {

        Throwable runtimeExResult;
        Throwable checkedExResult;
        Throwable supplierExResult;

        final CountDownLatch executeBlockingLatch = new CountDownLatch(1);
        final AtomicReference<String> executeBlockingResult = new AtomicReference<>();
        final AtomicReference<String> executeBlockingThreadName = new AtomicReference<>();

        void onStart(@Observes StartupEvent event) {
            // Test 1: Uni failure with RuntimeException
            try {
                VertxContextSupport.subscribeAndAwait((Supplier<Uni<String>>) () -> Uni.createFrom()
                        .failure(new RuntimeException("expected runtime failure")));
            } catch (Throwable e) {
                runtimeExResult = e;
            }

            // Test 2: Uni failure with checked exception
            try {
                VertxContextSupport.subscribeAndAwait(
                        (Supplier<Uni<String>>) () -> Uni.createFrom().failure(new IOException("expected checked failure")));
            } catch (Throwable e) {
                checkedExResult = e;
            }

            // Test 3: Exception thrown in the supplier itself (not from Uni)
            try {
                VertxContextSupport.subscribeAndAwait((Supplier<Uni<String>>) () -> {
                    throw new IllegalArgumentException("supplier failure");
                });
            } catch (Throwable e) {
                supplierExResult = e;
            }

            // Test 4: executeBlocking success + thread verification
            VertxContextSupport.executeBlocking(() -> {
                executeBlockingThreadName.set(Thread.currentThread().getName());
                return "blocking-result";
            }).subscribe().with(
                    result -> {
                        executeBlockingResult.set(result);
                        executeBlockingLatch.countDown();
                    },
                    failure -> {
                        executeBlockingLatch.countDown();
                    });
        }
    }
}
