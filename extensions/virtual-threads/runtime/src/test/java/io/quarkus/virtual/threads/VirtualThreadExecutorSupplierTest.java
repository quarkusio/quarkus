package io.quarkus.virtual.threads;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

class VirtualThreadExecutorSupplierTest {

    @BeforeEach
    void configRecorder() {
        VirtualThreadsRecorder.config = new SmallRyeConfigBuilder()
                .addDiscoveredConverters()
                .withMapping(VirtualThreadsConfig.class)
                .build().getConfigMapping(VirtualThreadsConfig.class);
    }

    @Test
    void virtualThreadCustomScheduler()
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Executor executor = VirtualThreadsRecorder.newVirtualThreadPerTaskExecutorWithName("vthread-");
        var assertSubscriber = Uni.createFrom().emitter(e -> {
            assertThat(Thread.currentThread().getName()).isNotEmpty()
                    .startsWith("vthread-");
            assertThatItRunsOnVirtualThread();
            e.complete(null);
        }).runSubscriptionOn(executor)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertSubscriber.awaitItem(Duration.ofSeconds(1)).assertCompleted();
    }

    @Test
    void execute() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Executor executor = VirtualThreadsRecorder.newVirtualThreadPerTaskExecutorWithName(null);
        var assertSubscriber = Uni.createFrom().emitter(e -> {
            assertThat(Thread.currentThread().getName()).isEmpty();
            assertThatItRunsOnVirtualThread();
            e.complete(null);
        }).runSubscriptionOn(executor)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertSubscriber.awaitItem(Duration.ofSeconds(1)).assertCompleted();
    }

    @Test
    void executePropagatesVertxContext() throws ExecutionException, InterruptedException {
        ExecutorService executorService = VirtualThreadsRecorder.getCurrent();
        Vertx vertx = Vertx.vertx();
        CompletableFuture<Context> future = new CompletableFuture<>();
        vertx.executeBlocking(() -> {
            executorService.execute(() -> {
                assertThatItRunsOnVirtualThread();
                future.complete(Vertx.currentContext());
            });
            return null;
        }, false).toCompletionStage().toCompletableFuture().get();
        assertThat(future.get()).isNotNull();
    }

    @Test
    void executePropagatesVertxContextMutiny() {
        ExecutorService executorService = VirtualThreadsRecorder.getCurrent();
        Vertx vertx = Vertx.vertx();
        var assertSubscriber = Uni.createFrom().voidItem()
                .runSubscriptionOn(command -> vertx.executeBlocking(() -> {
                    command.run();
                    return null;
                }, false))
                .emitOn(executorService)
                .map(x -> {
                    assertThatItRunsOnVirtualThread();
                    return Vertx.currentContext();
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(assertSubscriber.awaitItem().assertCompleted().getItem()).isNotNull();
    }

    @Test
    void submitPropagatesVertxContext() throws ExecutionException, InterruptedException {
        ExecutorService executorService = VirtualThreadsRecorder.getCurrent();
        Vertx vertx = Vertx.vertx();
        CompletableFuture<Context> future = new CompletableFuture<>();
        vertx.executeBlocking(() -> {
            executorService.submit(() -> {
                assertThatItRunsOnVirtualThread();
                future.complete(Vertx.currentContext());
            }, false);
            return null;
        }).toCompletionStage().toCompletableFuture().get();
        assertThat(future.get()).isNotNull();
    }

    @Test
    void invokeAllPropagatesVertxContext() throws ExecutionException, InterruptedException {
        ExecutorService executorService = VirtualThreadsRecorder.getCurrent();
        Vertx vertx = Vertx.vertx();
        List<Future<Context>> futures = vertx.executeBlocking(() -> {
            return executorService.invokeAll(List.of((Callable<Context>) () -> {
                assertThatItRunsOnVirtualThread();
                return Vertx.currentContext();
            }, (Callable<Context>) () -> {
                assertThatItRunsOnVirtualThread();
                return Vertx.currentContext();
            }));
        }, false).toCompletionStage().toCompletableFuture().get();
        assertThat(futures).allSatisfy(contextFuture -> assertThat(contextFuture.get()).isNotNull());
    }

    public static void assertThatItRunsOnVirtualThread() {
        if (!Thread.currentThread().isVirtual()) {
            throw new AssertionError("Thread " + Thread.currentThread() + " is not a virtual thread");
        }
    }
}
