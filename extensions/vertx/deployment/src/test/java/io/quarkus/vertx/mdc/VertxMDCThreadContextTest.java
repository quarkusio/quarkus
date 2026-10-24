package io.quarkus.vertx.mdc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.VertxContextSupport;
import io.quarkus.vertx.core.runtime.VertxMDC;

/**
 * Tests that the MDC follows {@link ThreadContext} contextualization onto executors that Quarkus does not manage.
 */
public class VertxMDCThreadContextTest {

    private static final String KEY = "requestId";

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest().withEmptyApplication();

    private static final ExecutorService FOREIGN_EXECUTOR = Executors.newSingleThreadExecutor();

    @Inject
    ThreadContext threadContext;

    @Inject
    ManagedExecutor managedExecutor;

    @AfterEach
    void clearMdc() {
        VertxMDC.INSTANCE.clear();
    }

    @AfterAll
    static void shutdownExecutor() {
        FOREIGN_EXECUTOR.shutdownNow();
    }

    @Test
    void contextualRunnablePropagatesMdcFromVertxContext() throws Exception {
        String seen = VertxContextSupport.executeBlocking(() -> {
            VertxMDC.INSTANCE.put(KEY, "from-vertx");
            AtomicReference<String> ref = new AtomicReference<>();
            FOREIGN_EXECUTOR.submit(threadContext.contextualRunnable(() -> ref.set(VertxMDC.INSTANCE.get(KEY)))).get();
            return ref.get();
        }).subscribe().asCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        assertEquals("from-vertx", seen);
    }

    @Test
    void contextualRunnablePropagatesMdcFromPlainThread() throws Exception {
        VertxMDC.INSTANCE.put(KEY, "from-plain-thread");
        AtomicReference<String> ref = new AtomicReference<>();
        FOREIGN_EXECUTOR.submit(threadContext.contextualRunnable(() -> ref.set(VertxMDC.INSTANCE.get(KEY)))).get();
        assertEquals("from-plain-thread", ref.get());
    }

    @Test
    void withContextCapturePropagatesMdc() throws Exception {
        VertxMDC.INSTANCE.put(KEY, "captured");
        AtomicReference<String> ref = new AtomicReference<>();
        threadContext.withContextCapture(CompletableFuture.completedFuture(null))
                .thenRunAsync(() -> ref.set(VertxMDC.INSTANCE.get(KEY)), FOREIGN_EXECUTOR)
                .toCompletableFuture().get(5, TimeUnit.SECONDS);
        assertEquals("captured", ref.get());
    }

    @Test
    void mdcOfTheTargetThreadIsRestoredAfterTheTask() throws Exception {
        FOREIGN_EXECUTOR.submit(() -> VertxMDC.INSTANCE.put(KEY, "own")).get();
        VertxMDC.INSTANCE.put(KEY, "propagated");
        AtomicReference<String> inside = new AtomicReference<>();
        FOREIGN_EXECUTOR.submit(threadContext.contextualRunnable(() -> inside.set(VertxMDC.INSTANCE.get(KEY)))).get();
        AtomicReference<String> after = new AtomicReference<>();
        FOREIGN_EXECUTOR.submit(() -> after.set(VertxMDC.INSTANCE.get(KEY))).get();
        assertEquals("propagated", inside.get());
        assertEquals("own", after.get());
    }

    @Test
    void changesInsideTheTaskDoNotLeakToTheCaller() throws Exception {
        VertxMDC.INSTANCE.put(KEY, "caller");
        FOREIGN_EXECUTOR.submit(threadContext.contextualRunnable(() -> VertxMDC.INSTANCE.put(KEY, "task"))).get();
        assertEquals("caller", VertxMDC.INSTANCE.get(KEY));
    }

    @Test
    void clearedMdcIsEmptyInsideTheTask() throws Exception {
        ThreadContext clearing = ThreadContext.builder()
                .propagated(ThreadContext.NONE)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();
        VertxMDC.INSTANCE.put(KEY, "caller");
        AtomicReference<String> ref = new AtomicReference<>("unset");
        FOREIGN_EXECUTOR.submit(clearing.contextualRunnable(() -> ref.set(VertxMDC.INSTANCE.get(KEY)))).get();
        assertNull(ref.get());
        assertEquals("caller", VertxMDC.INSTANCE.get(KEY));
    }

    @Test
    void managedExecutorStillPropagatesMdcFromVertxContext() throws Exception {
        String seen = VertxContextSupport.executeBlocking(() -> {
            VertxMDC.INSTANCE.put(KEY, "managed");
            AtomicReference<String> ref = new AtomicReference<>();
            managedExecutor.submit(() -> {
                ref.set(VertxMDC.INSTANCE.get(KEY));
                VertxMDC.INSTANCE.put(KEY, "task");
            }).get(5, TimeUnit.SECONDS);
            return ref.get() + "/" + VertxMDC.INSTANCE.get(KEY);
        }).subscribe().asCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        assertEquals("managed/managed", seen);
    }
}
