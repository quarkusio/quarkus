package io.quarkus.virtual.threads;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

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
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
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
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
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
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
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
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
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
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
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
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
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

    @Test
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
    void boundedExecutorLimitsConcurrencyViaExecute() throws Exception {
        int maxConcurrency = 3;
        int totalTasks = 10;
        ExecutorService rawExecutor = VirtualThreadsRecorder.getCurrent();
        BoundedExecutorService bounded = new BoundedExecutorService(rawExecutor, maxConcurrency);

        AtomicInteger running = new AtomicInteger(0);
        AtomicInteger peak = new AtomicInteger(0);
        CountDownLatch allDone = new CountDownLatch(totalTasks);

        for (int i = 0; i < totalTasks; i++) {
            bounded.execute(() -> {
                int current = running.incrementAndGet();
                peak.accumulateAndGet(current, Math::max);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    running.decrementAndGet();
                    allDone.countDown();
                }
            });
        }

        assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(peak.get()).isLessThanOrEqualTo(maxConcurrency);
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
    void boundedExecutorLimitsConcurrencyViaSubmitRunnable() throws Exception {
        int maxConcurrency = 3;
        int totalTasks = 10;
        ExecutorService rawExecutor = VirtualThreadsRecorder.getCurrent();
        BoundedExecutorService bounded = new BoundedExecutorService(rawExecutor, maxConcurrency);

        AtomicInteger running = new AtomicInteger(0);
        AtomicInteger peak = new AtomicInteger(0);
        CountDownLatch allDone = new CountDownLatch(totalTasks);

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < totalTasks; i++) {
            futures.add(bounded.submit(() -> {
                int current = running.incrementAndGet();
                peak.accumulateAndGet(current, Math::max);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    running.decrementAndGet();
                    allDone.countDown();
                }
            }));
        }

        assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(peak.get()).isLessThanOrEqualTo(maxConcurrency);
        for (Future<?> f : futures) {
            f.get(1, TimeUnit.SECONDS);
        }
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
    void boundedExecutorLimitsConcurrencyViaSubmitCallable() throws Exception {
        int maxConcurrency = 3;
        int totalTasks = 10;
        ExecutorService rawExecutor = VirtualThreadsRecorder.getCurrent();
        BoundedExecutorService bounded = new BoundedExecutorService(rawExecutor, maxConcurrency);

        AtomicInteger running = new AtomicInteger(0);
        AtomicInteger peak = new AtomicInteger(0);
        CountDownLatch allDone = new CountDownLatch(totalTasks);

        List<Future<Integer>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < totalTasks; i++) {
            final int taskId = i;
            futures.add(bounded.submit(() -> {
                int current = running.incrementAndGet();
                peak.accumulateAndGet(current, Math::max);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                } finally {
                    running.decrementAndGet();
                    allDone.countDown();
                }
                return taskId;
            }));
        }

        assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(peak.get()).isLessThanOrEqualTo(maxConcurrency);
        for (int i = 0; i < totalTasks; i++) {
            assertThat(futures.get(i).get(1, TimeUnit.SECONDS)).isEqualTo(i);
        }
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
    void boundedExecutorLimitsConcurrencyViaInvokeAll() throws Exception {
        int maxConcurrency = 3;
        int totalTasks = 10;
        ExecutorService rawExecutor = VirtualThreadsRecorder.getCurrent();
        BoundedExecutorService bounded = new BoundedExecutorService(rawExecutor, maxConcurrency);

        AtomicInteger running = new AtomicInteger(0);
        AtomicInteger peak = new AtomicInteger(0);

        List<Callable<Integer>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < totalTasks; i++) {
            final int taskId = i;
            tasks.add(() -> {
                int current = running.incrementAndGet();
                peak.accumulateAndGet(current, Math::max);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                } finally {
                    running.decrementAndGet();
                }
                return taskId;
            });
        }

        List<Future<Integer>> results = bounded.invokeAll(tasks);

        assertThat(peak.get()).isLessThanOrEqualTo(maxConcurrency);
        assertThat(results).hasSize(totalTasks);
        for (int i = 0; i < totalTasks; i++) {
            assertThat(results.get(i).get()).isEqualTo(i);
        }
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
    void boundedExecutorLimitsConcurrencyViaInvokeAny() throws Exception {
        int maxConcurrency = 2;
        ExecutorService rawExecutor = VirtualThreadsRecorder.getCurrent();
        BoundedExecutorService bounded = new BoundedExecutorService(rawExecutor, maxConcurrency);

        AtomicInteger running = new AtomicInteger(0);
        AtomicInteger peak = new AtomicInteger(0);

        List<Callable<String>> tasks = List.of(
                () -> {
                    int current = running.incrementAndGet();
                    peak.accumulateAndGet(current, Math::max);
                    try {
                        Thread.sleep(50);
                        return "first";
                    } finally {
                        running.decrementAndGet();
                    }
                },
                () -> {
                    int current = running.incrementAndGet();
                    peak.accumulateAndGet(current, Math::max);
                    try {
                        Thread.sleep(50);
                        return "second";
                    } finally {
                        running.decrementAndGet();
                    }
                },
                () -> {
                    int current = running.incrementAndGet();
                    peak.accumulateAndGet(current, Math::max);
                    try {
                        Thread.sleep(50);
                        return "third";
                    } finally {
                        running.decrementAndGet();
                    }
                });

        String result = bounded.invokeAny(tasks);

        assertThat(result).isIn("first", "second", "third");
        assertThat(peak.get()).isLessThanOrEqualTo(maxConcurrency);
    }

    public static void assertThatItRunsOnVirtualThread() {
        // We cannot depend on a Java 20.
        try {
            Method isVirtual = Thread.class.getMethod("isVirtual");
            isVirtual.setAccessible(true);
            boolean virtual = (Boolean) isVirtual.invoke(Thread.currentThread());
            if (!virtual) {
                throw new AssertionError("Thread " + Thread.currentThread() + " is not a virtual thread");
            }
        } catch (Exception e) {
            throw new AssertionError(
                    "Thread " + Thread.currentThread() + " is not a virtual thread - cannot invoke Thread.isVirtual()", e);
        }
    }
}
