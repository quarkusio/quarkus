package io.quarkus.cache.test.runtime;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

public class DuplicatedContextHandlingTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(jar -> jar
            .addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Inject
    Vertx vertx;

    @Test
    @ActivateRequestContext
    void testDuplicatedContextHandlingWhenCalledFromNoContext() {
        cachedService.direct(false).await().indefinitely();
        cachedService.direct(true).await().indefinitely();
    }

    @Test
    @ActivateRequestContext
    void testDuplicatedContextHandlingWhenCalledOnContext() throws InterruptedException {
        ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
        if (context.isDuplicate()) {
            context = context.duplicate();
        }

        CountDownLatch latch = new CountDownLatch(1);
        Context tmp = context;
        context.runOnContext(x -> {
            cachedService.direct(false)
                    .invoke(() -> {
                        if (!tmp.equals(Vertx.currentContext())) {
                            throw new AssertionError("Expected to go back on the caller context");
                        }
                    })
                    .subscribe().with(y -> latch.countDown());
        });
        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));

        CountDownLatch latch2 = new CountDownLatch(1);
        context.runOnContext(x -> {
            cachedService.direct(true)
                    .invoke(() -> {
                        if (!tmp.equals(Vertx.currentContext())) {
                            throw new AssertionError("Expected to go back on the caller context");
                        }
                    })
                    .subscribe().with(y -> latch2.countDown());
        });
        Assertions.assertTrue(latch2.await(1, TimeUnit.SECONDS));

        CountDownLatch latch3 = new CountDownLatch(1);
        context.runOnContext(x -> {
            cachedService.direct(false)
                    .invoke(() -> {
                        if (!tmp.equals(Vertx.currentContext())) {
                            throw new AssertionError("Expected to go back on the caller context");
                        }
                    })
                    .subscribe().with(y -> latch3.countDown());
        });
        Assertions.assertTrue(latch3.await(1, TimeUnit.SECONDS));

    }

    @Test
    @ActivateRequestContext
    void testDuplicatedContextHandlingWhenCalledOnDifferentContexts() throws InterruptedException {
        ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
        context = context.duplicate();
        var context2 = context.duplicate();

        CountDownLatch latch = new CountDownLatch(1);
        Context tmp = context;
        context.runOnContext(x -> {
            cachedService.direct(false)
                    .invoke(() -> {
                        if (!tmp.equals(Vertx.currentContext())) {
                            throw new AssertionError("Expected to go back on the caller context");
                        }
                    })
                    .subscribe().with(y -> latch.countDown());
        });
        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));

        CountDownLatch latch2 = new CountDownLatch(1);
        context2.runOnContext(x -> {
            cachedService.direct(false)
                    .invoke(() -> {
                        if (!context2.equals(Vertx.currentContext())) {
                            throw new AssertionError("Expected to go back on the caller context");
                        }
                    })
                    .subscribe().with(y -> latch2.countDown());
        });
        Assertions.assertTrue(latch2.await(1, TimeUnit.SECONDS));
    }

    @Test
    @ActivateRequestContext
    void testDuplicatedContextHandlingWhenCalledContextAndAnsweredFromAnotherContext() throws InterruptedException {
        ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
        context = context.duplicate();
        var context2 = context.duplicate();

        CountDownLatch latch = new CountDownLatch(1);
        Context tmp = context;
        context.runOnContext(x -> {
            cachedService.directOnAnotherContext(false)
                    .invoke(() -> {
                        if (!tmp.equals(Vertx.currentContext())) {
                            throw new AssertionError("Expected to go back on the caller context");
                        }
                    })
                    .subscribe().with(y -> latch.countDown());
        });
        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));

        CountDownLatch latch2 = new CountDownLatch(1);
        context2.runOnContext(x -> {
            cachedService.directOnAnotherContext(false)
                    .invoke(() -> {
                        if (!context2.equals(Vertx.currentContext())) {
                            throw new AssertionError("Expected to go back on the caller context");
                        }
                    })
                    .subscribe().with(y -> latch2.countDown());
        });
        Assertions.assertTrue(latch2.await(1, TimeUnit.SECONDS));
    }

    @RepeatedTest(10)
    void testWithAsyncTaskRestoringContext() throws InterruptedException {
        var rootContext = vertx.getOrCreateContext();
        var duplicatedContext1 = ((ContextInternal) rootContext).duplicate();

        CountDownLatch latch = new CountDownLatch(1);
        duplicatedContext1.runOnContext(x -> {
            cachedService.async()
                    .subscribeAsCompletionStage()
                    .whenComplete((s, t) -> {
                        Assertions.assertEquals(duplicatedContext1, Vertx.currentContext());
                        latch.countDown();
                    });
        });

        var duplicatedContext2 = ((ContextInternal) rootContext).duplicate();
        CountDownLatch latch2 = new CountDownLatch(1);
        duplicatedContext2.runOnContext(x -> {
            cachedService.async()
                    .subscribeAsCompletionStage()
                    .whenComplete((s, t) -> {
                        Assertions.assertEquals(duplicatedContext2, Vertx.currentContext());
                        latch2.countDown();
                    });
        });

        Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS));
        Assertions.assertTrue(latch2.await(2, TimeUnit.SECONDS));
    }

    @ApplicationScoped
    public static class CachedService {

        volatile boolean timedout = false;

        @CacheResult(cacheName = "duplicated-context-cache", lockTimeout = 100)
        public Uni<String> direct(boolean timeout) {
            if (!timeout || timedout) {
                return Uni.createFrom().item("foo");
            }
            timedout = true;
            return Uni.createFrom().nothing();
        }

        @CacheResult(cacheName = "duplicated-context-cache", lockTimeout = 100)
        public Uni<String> async() {
            Context context = Vertx.currentContext();
            return Uni.createFrom().item("foo")
                    .onItem().delayIt().by(Duration.ofMillis(10))
                    .map(s -> s.toUpperCase())
                    .emitOn(runnable -> context.runOnContext(x -> runnable.run()));
        }

        @CacheResult(cacheName = "duplicated-context-cache", lockTimeout = 100)
        public Uni<String> directOnAnotherContext(boolean timeout) {
            if (!timeout || timedout) {
                return Uni.createFrom().item("foo")
                        .emitOn(c -> ((ContextInternal) Vertx.currentContext().owner()).duplicate().runOnContext(x -> c.run()));
            }
            timedout = true;
            return Uni.createFrom().nothing();
        }
    }

}
