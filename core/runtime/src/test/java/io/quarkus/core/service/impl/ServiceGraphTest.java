package io.quarkus.core.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.shutdown.ShutdownConfig;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.quarkus.runtime.shutdown.ShutdownRecorder;

/**
 * Tests for {@link ServiceNode} and {@link ServiceGraph} lifecycle.
 * <p>
 * Tests verify the state machine transitions, dependency satisfaction,
 * failure cascade, stop cascade, graceful shutdown, and executor swap behaviors.
 */
class ServiceGraphTest {

    // ═══════════════════════════════════════════════
    // Action handles for test services
    // ═══════════════════════════════════════════════

    /**
     * Action that completes with a string value.
     *
     * @param node the service node
     */
    static void typedAction(ServiceNode node) {
        node.startComplete("value-from-" + node.name());
    }

    /**
     * Action that completes as void.
     *
     * @param node the service node
     */
    static void voidAction(ServiceNode node) {
        node.startComplete();
    }

    /**
     * Action that fails immediately.
     *
     * @param node the service node
     */
    static void failingAction(ServiceNode node) {
        node.startFailed(new RuntimeException("deliberate failure in " + node.name()));
    }

    /**
     * Action that reads dependency values.
     *
     * @param node the service node
     */
    static void depReadingAction(ServiceNode node) {
        Object dep0 = node.dependencyValue(0);
        node.startComplete("got-" + dep0);
    }

    /**
     * Top sentinel action: registers stop-done handler, then completes.
     *
     * @param node the service node
     */
    static void topSentinel(ServiceNode node) {
        node.onStop(() -> node.graph().signalStopDone());
        node.startComplete();
    }

    /**
     * Bottom sentinel action: signals start-done, then completes.
     *
     * @param node the service node
     */
    static void bottomSentinel(ServiceNode node) {
        node.graph().signalStartDone();
        node.startComplete();
    }

    /** MethodHandle pointing to {@link #typedAction(ServiceNode)}. */
    private static final MethodHandle MH_TYPED;

    /** MethodHandle pointing to {@link #voidAction(ServiceNode)}. */
    private static final MethodHandle MH_VOID;

    /** MethodHandle pointing to {@link #failingAction(ServiceNode)}. */
    private static final MethodHandle MH_FAILING;

    /** MethodHandle pointing to {@link #depReadingAction(ServiceNode)}. */
    private static final MethodHandle MH_DEP_READING;

    /** MethodHandle pointing to {@link #topSentinel(ServiceNode)}. */
    private static final MethodHandle MH_TOP;

    /** MethodHandle pointing to {@link #bottomSentinel(ServiceNode)}. */
    private static final MethodHandle MH_BOTTOM;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType mt = MethodType.methodType(void.class, ServiceNode.class);
            MH_TYPED = lookup.findStatic(ServiceGraphTest.class, "typedAction", mt);
            MH_VOID = lookup.findStatic(ServiceGraphTest.class, "voidAction", mt);
            MH_FAILING = lookup.findStatic(ServiceGraphTest.class, "failingAction", mt);
            MH_DEP_READING = lookup.findStatic(ServiceGraphTest.class, "depReadingAction", mt);
            MH_TOP = lookup.findStatic(ServiceGraphTest.class, "topSentinel", mt);
            MH_BOTTOM = lookup.findStatic(ServiceGraphTest.class, "bottomSentinel", mt);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ═══════════════════════════════════════════════
    // Basic lifecycle
    // ═══════════════════════════════════════════════

    /**
     * Verify that a single service node starts and stops cleanly with sentinels.
     */
    @Test
    void singleNodeStartsAndStops() {
        ServiceGraph graph = new ServiceGraph();
        ServiceNode top = new ServiceNode("top", MH_TOP, graph, 1);
        ServiceNode svc = new ServiceNode("svc", MH_VOID, graph, 1, top);
        ServiceNode bottom = new ServiceNode("bottom", MH_BOTTOM, graph, 0, svc);
        graph.setTop(top);
        graph.setBottom(bottom);

        graph.start();

        assertThat(top.state()).isEqualTo(ServiceNode.S_COMPLETED);
        assertThat(svc.state()).isEqualTo(ServiceNode.S_COMPLETED);
        assertThat(bottom.state()).isEqualTo(ServiceNode.S_COMPLETED);

        graph.stop();

        assertThat(bottom.state()).isEqualTo(ServiceNode.S_STOPPED);
        assertThat(svc.state()).isEqualTo(ServiceNode.S_STOPPED);
        assertThat(top.state()).isEqualTo(ServiceNode.S_STOPPED);
    }

    /**
     * Verify that a typed service produces and stores its expected return value.
     */
    @Test
    void typedServiceProducesValue() {
        ServiceGraph graph = new ServiceGraph();
        ServiceNode top = new ServiceNode("top", MH_TOP, graph, 1);
        ServiceNode svc = new ServiceNode("svc", MH_TYPED, graph, 1, top);
        ServiceNode bottom = new ServiceNode("bottom", MH_BOTTOM, graph, 0, svc);
        graph.setTop(top);
        graph.setBottom(bottom);

        graph.start();

        assertThat(svc.value()).isEqualTo("value-from-svc");
    }

    /**
     * Verify that a dependent service can access the value produced by its dependency.
     */
    @Test
    void dependencyValueAccessWorks() {
        ServiceGraph graph = new ServiceGraph();
        ServiceNode top = new ServiceNode("top", MH_TOP, graph, 1);
        ServiceNode producer = new ServiceNode("producer", MH_TYPED, graph, 1, top);
        // depReadingAction reads dependencyValue(0), which is the first dep after top
        // but dep index 0 = top (first in constructor), dep index 1 = producer
        // Actually, we need producer at index 0 for depReadingAction to read it.
        // Let's put producer first in the dep list.
        ServiceNode consumer = new ServiceNode("consumer", MH_DEP_READING, graph, 1,
                List.of(producer, top));
        ServiceNode bottom = new ServiceNode("bottom", MH_BOTTOM, graph, 0, consumer);
        graph.setTop(top);
        graph.setBottom(bottom);

        graph.start();

        assertThat(consumer.value()).isEqualTo("got-value-from-producer");
    }

    // ═══════════════════════════════════════════════
    // Diamond topology
    // ═══════════════════════════════════════════════

    /**
     * Verify that a diamond dependency graph starts all services successfully.
     */
    @Test
    void diamondTopologyCompletesAllNodes() {
        ServiceGraph graph = new ServiceGraph();
        ServiceNode top = new ServiceNode("top", MH_TOP, graph, 1);
        ServiceNode a = new ServiceNode("A", MH_VOID, graph, 2, top);
        ServiceNode b = new ServiceNode("B", MH_VOID, graph, 1, a);
        ServiceNode c = new ServiceNode("C", MH_VOID, graph, 1, a);
        ServiceNode d = new ServiceNode("D", MH_VOID, graph, 1, b, c);
        ServiceNode bottom = new ServiceNode("bottom", MH_BOTTOM, graph, 0, d);
        graph.setTop(top);
        graph.setBottom(bottom);

        graph.start();

        assertThat(a.state()).isEqualTo(ServiceNode.S_COMPLETED);
        assertThat(b.state()).isEqualTo(ServiceNode.S_COMPLETED);
        assertThat(c.state()).isEqualTo(ServiceNode.S_COMPLETED);
        assertThat(d.state()).isEqualTo(ServiceNode.S_COMPLETED);
    }

    /**
     * Verify that a diamond dependency graph stops all services in reverse topological order.
     */
    @Test
    void diamondStopsInReverseOrder() {
        List<String> stopOrder = Collections.synchronizedList(new ArrayList<>());
        ServiceGraph graph = new ServiceGraph();
        ServiceNode top = new ServiceNode("top", MH_TOP, graph, 1);
        ServiceNode a = new ServiceNode("A", makeStopTracker("A", stopOrder), graph, 2, top);
        ServiceNode b = new ServiceNode("B", makeStopTracker("B", stopOrder), graph, 1, a);
        ServiceNode c = new ServiceNode("C", makeStopTracker("C", stopOrder), graph, 1, a);
        ServiceNode d = new ServiceNode("D", makeStopTracker("D", stopOrder), graph, 1, b, c);
        ServiceNode bottom = new ServiceNode("bottom", MH_BOTTOM, graph, 0, d);
        graph.setTop(top);
        graph.setBottom(bottom);

        graph.start();
        graph.stop();

        // D must stop before B and C; B and C must stop before A
        assertThat(stopOrder.indexOf("D")).isLessThan(stopOrder.indexOf("B"));
        assertThat(stopOrder.indexOf("D")).isLessThan(stopOrder.indexOf("C"));
        assertThat(stopOrder.indexOf("B")).isLessThan(stopOrder.indexOf("A"));
        assertThat(stopOrder.indexOf("C")).isLessThan(stopOrder.indexOf("A"));
    }

    // ═══════════════════════════════════════════════
    // Failure scenarios
    // ═══════════════════════════════════════════════

    /**
     * Verify that a failure in a service cancels its unstarted downstream dependents.
     */
    @Test
    void failureCancelsDependents() {
        ServiceGraph graph = new ServiceGraph();
        ServiceNode top = new ServiceNode("top", MH_TOP, graph, 1);
        ServiceNode a = new ServiceNode("A", MH_FAILING, graph, 1, top);
        ServiceNode b = new ServiceNode("B", MH_VOID, graph, 1, a);
        ServiceNode bottom = new ServiceNode("bottom", MH_BOTTOM, graph, 0, b);
        graph.setTop(top);
        graph.setBottom(bottom);

        try {
            graph.start();
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage()).contains("deliberate failure");
        }

        assertThat(a.state()).isEqualTo(ServiceNode.S_FAILED_STOPPED);
        assertThat(b.state()).isEqualTo(ServiceNode.S_CANCELED);
        assertThat(a.failure()).isNotNull();
        assertThat(a.failure().getMessage()).contains("deliberate failure");
    }

    /**
     * Verify that failure in one branch of a diamond topology cancels downstream nodes
     * while stopping already completed nodes in reverse order.
     */
    @Test
    void failureInDiamondCancelsDownstreamButCompletedNodesStop() {
        List<String> stopOrder = Collections.synchronizedList(new ArrayList<>());
        ServiceGraph graph = new ServiceGraph();
        ServiceNode top = new ServiceNode("top", MH_TOP, graph, 1);
        ServiceNode a = new ServiceNode("A", makeStopTracker("A", stopOrder), graph, 2, top);
        ServiceNode b = new ServiceNode("B", MH_FAILING, graph, 1, a); // B fails
        ServiceNode c = new ServiceNode("C", makeStopTracker("C", stopOrder), graph, 1, a); // C completes
        ServiceNode d = new ServiceNode("D", MH_VOID, graph, 1, b, c); // D depends on B → canceled
        ServiceNode bottom = new ServiceNode("bottom", MH_BOTTOM, graph, 0, d);
        graph.setTop(top);
        graph.setBottom(bottom);

        try {
            graph.start();
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage()).contains("deliberate failure");
        }

        assertThat(b.state()).isEqualTo(ServiceNode.S_FAILED_STOPPED);
        assertThat(d.state()).isEqualTo(ServiceNode.S_CANCELED);
        // A and C completed and should be stopped
        assertThat(c.state()).isEqualTo(ServiceNode.S_STOPPED);
        assertThat(a.state()).isEqualTo(ServiceNode.S_STOPPED);
        // stop order: C before A (C depends on A)
        assertThat(stopOrder.indexOf("C")).isLessThan(stopOrder.indexOf("A"));
    }

    // ═══════════════════════════════════════════════
    // Stop handler
    // ═══════════════════════════════════════════════

    /**
     * Verify that registered stop handlers are executed when the graph stops.
     */
    @Test
    void stopHandlerIsCalled() {
        List<String> stopped = new ArrayList<>();
        ServiceGraph graph = new ServiceGraph();
        ServiceNode top = new ServiceNode("top", MH_TOP, graph, 1);
        ServiceNode svc = new ServiceNode("svc", MH_VOID, graph, 1, top);
        ServiceNode bottom = new ServiceNode("bottom", MH_BOTTOM, graph, 0, svc);
        graph.setTop(top);
        graph.setBottom(bottom);

        graph.start();

        // register stop handler manually (normally done by the action)
        svc.onStop(() -> stopped.add("svc-stopped"));

        graph.stop();

        assertThat(stopped).containsExactly("svc-stopped");
    }

    /**
     * Verify that attempting to register multiple stop handlers on a single node throws an exception.
     */
    @Test
    void duplicateStopHandlerThrows() {
        ServiceGraph graph = new ServiceGraph();
        ServiceNode top = new ServiceNode("top", MH_TOP, graph, 1);
        ServiceNode svc = new ServiceNode("svc", MH_VOID, graph, 1, top);
        ServiceNode bottom = new ServiceNode("bottom", MH_BOTTOM, graph, 0, svc);
        graph.setTop(top);
        graph.setBottom(bottom);

        graph.start();
        svc.onStop(() -> {
        });

        assertThatThrownBy(() -> svc.onStop(() -> {
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stop handler already registered");
    }

    // ═══════════════════════════════════════════════
    // Graceful shutdown
    // ═══════════════════════════════════════════════

    /**
     * Verify that graceful pre-shutdown handlers execute before graceful shutdown handlers,
     * and both execute before service node stop handlers during teardown.
     */
    @Test
    void gracefulShutdownPhaseOrder() {
        ServiceGraph graph = new ServiceGraph();
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        createStandardGraph(graph, makeAction(node -> {
            node.onGracefulPreShutdown(() -> events.add("preShutdown"));
            node.onGracefulShutdown(() -> events.add("shutdown"));
            node.onStop(() -> events.add("stop"));
            node.startComplete();
        }));

        graph.start();
        graph.stop();

        assertThat(events).containsExactly("preShutdown", "shutdown", "stop");
    }

    /**
     * Verify that graceful pre-shutdown and shutdown handlers registered via
     * {@link io.quarkus.core.StartContext} synchronous and asynchronous methods
     * all execute in the expected phase order.
     */
    @Test
    void gracefulShutdownViaServiceNodeContextMethods() {
        ServiceGraph graph = new ServiceGraph();
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        createStandardGraph(graph, makeAction(node -> {
            node.onGracefulPreShutdown(() -> events.add("syncPreShutdown"));
            node.onGracefulPreShutdownAsync(ctxt -> {
                events.add("asyncPreShutdown");
                ctxt.done();
            });
            node.onGracefulShutdown(() -> events.add("syncShutdown"));
            node.onGracefulShutdownAsync(ctxt -> {
                events.add("asyncShutdown");
                ctxt.done();
            });
            node.onStop(() -> events.add("stop"));
            node.startComplete();
        }));

        graph.start();
        graph.stop();

        // All pre-shutdown handlers must run before all shutdown handlers,
        // and all shutdown handlers must run before service stop.
        assertThat(events).contains("syncPreShutdown", "asyncPreShutdown", "syncShutdown", "asyncShutdown", "stop");
        assertThat(events.indexOf("syncPreShutdown")).isLessThan(events.indexOf("syncShutdown"));
        assertThat(events.indexOf("syncPreShutdown")).isLessThan(events.indexOf("asyncShutdown"));
        assertThat(events.indexOf("asyncPreShutdown")).isLessThan(events.indexOf("syncShutdown"));
        assertThat(events.indexOf("asyncPreShutdown")).isLessThan(events.indexOf("asyncShutdown"));
        assertThat(events.indexOf("syncShutdown")).isLessThan(events.indexOf("stop"));
        assertThat(events.indexOf("asyncShutdown")).isLessThan(events.indexOf("stop"));
    }

    /**
     * Verify that handlers registered directly on {@link ServiceGraph} via
     * {@link ServiceGraph#addGracefulPreShutdownHandler} and
     * {@link ServiceGraph#addGracefulShutdownHandler} are executed in order.
     */
    @Test
    void gracefulShutdownDirectGraphRegistration() {
        ServiceGraph graph = new ServiceGraph();
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        graph.addGracefulPreShutdownHandler(ctxt -> {
            events.add("directPreShutdown");
            ctxt.done();
        });
        graph.addGracefulShutdownHandler(ctxt -> {
            events.add("directShutdown");
            ctxt.done();
        });
        createStandardGraph(graph, MH_VOID);

        graph.start();
        graph.stop();

        assertThat(events).containsExactly("directPreShutdown", "directShutdown");
    }

    /**
     * Verify that asynchronous pre-shutdown and shutdown handlers delay teardown
     * until their respective {@link io.quarkus.core.AsyncGracefulShutdownContext#done()}
     * methods are called from background threads.
     */
    @Test
    void gracefulShutdownAsyncCompletionAwaited() {
        ServiceGraph graph = new ServiceGraph();
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        ExecutorService bgExecutor = Executors.newCachedThreadPool();
        try {
            createStandardGraph(graph, makeAction(node -> {
                node.onGracefulPreShutdownAsync(ctxt -> {
                    bgExecutor.submit(() -> {
                        try {
                            Thread.sleep(30);
                            events.add("asyncPreShutdown");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            ctxt.done();
                        }
                    });
                });
                node.onGracefulShutdownAsync(ctxt -> {
                    bgExecutor.submit(() -> {
                        try {
                            Thread.sleep(30);
                            events.add("asyncShutdown");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            ctxt.done();
                        }
                    });
                });
                node.onStop(() -> events.add("stop"));
                node.startComplete();
            }));

            graph.start();
            graph.stop();

            assertThat(events).containsExactly("asyncPreShutdown", "asyncShutdown", "stop");
        } finally {
            bgExecutor.shutdownNow();
        }
    }

    /**
     * Verify that when shutdown delay is enabled, the configured delay occurs
     * between the completion of pre-shutdown handlers and the start of shutdown handlers.
     */
    @Test
    void gracefulShutdownDelayAppliedWhenConfigured() {
        ServiceGraph graph = new ServiceGraph();
        graph.setGracefulShutdownConfig(true, Duration.ofMillis(80), false, null);
        AtomicLong preShutdownEnd = new AtomicLong();
        AtomicLong shutdownStart = new AtomicLong();
        createStandardGraph(graph, makeAction(node -> {
            node.onGracefulPreShutdown(() -> preShutdownEnd.set(System.nanoTime()));
            node.onGracefulShutdown(() -> shutdownStart.set(System.nanoTime()));
            node.startComplete();
        }));

        graph.start();
        graph.stop();

        long elapsedMillis = Duration.ofNanos(shutdownStart.get() - preShutdownEnd.get()).toMillis();
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(60);
    }

    /**
     * Verify that when shutdown delay is disabled, no delay occurs between pre-shutdown
     * and shutdown handlers even if a delay duration was specified.
     */
    @Test
    void gracefulShutdownDelaySkippedWhenDisabled() {
        ServiceGraph graph = new ServiceGraph();
        graph.setGracefulShutdownConfig(false, Duration.ofMillis(500), false, null);
        AtomicLong preShutdownEnd = new AtomicLong();
        AtomicLong shutdownStart = new AtomicLong();
        createStandardGraph(graph, makeAction(node -> {
            node.onGracefulPreShutdown(() -> preShutdownEnd.set(System.nanoTime()));
            node.onGracefulShutdown(() -> shutdownStart.set(System.nanoTime()));
            node.startComplete();
        }));

        graph.start();
        graph.stop();

        long elapsedMillis = Duration.ofNanos(shutdownStart.get() - preShutdownEnd.get()).toMillis();
        assertThat(elapsedMillis).isLessThan(300);
    }

    /**
     * Verify that when shutdown delay is enabled but the duration is null or zero,
     * shutdown proceeds without delay.
     */
    @Test
    void gracefulShutdownDelayNullOrZeroSkipsDelay() {
        ServiceGraph graph = new ServiceGraph();
        graph.setGracefulShutdownConfig(true, Duration.ZERO, false, null);
        AtomicLong preShutdownEnd = new AtomicLong();
        AtomicLong shutdownStart = new AtomicLong();
        createStandardGraph(graph, makeAction(node -> {
            node.onGracefulPreShutdown(() -> preShutdownEnd.set(System.nanoTime()));
            node.onGracefulShutdown(() -> shutdownStart.set(System.nanoTime()));
            node.startComplete();
        }));

        graph.start();
        graph.stop();

        long elapsedMillis = Duration.ofNanos(shutdownStart.get() - preShutdownEnd.get()).toMillis();
        assertThat(elapsedMillis).isLessThan(200);
    }

    /**
     * Verify that graceful shutdown completes normally when shutdown handlers finish
     * within the configured timeout duration.
     */
    @Test
    void gracefulShutdownTimeoutAwaitsCompletion() {
        ServiceGraph graph = new ServiceGraph();
        graph.setGracefulShutdownConfig(false, null, true, Duration.ofSeconds(2));
        AtomicBoolean completed = new AtomicBoolean();
        createStandardGraph(graph, makeAction(node -> {
            node.onGracefulShutdownAsync(ctxt -> {
                completed.set(true);
                ctxt.done();
            });
            node.startComplete();
        }));

        graph.start();
        graph.stop();

        assertThat(completed).isTrue();
    }

    /**
     * Verify that when a graceful shutdown handler fails to complete within the
     * configured timeout duration, {@link ServiceGraph#stop()} logs an error and
     * continues with node teardown instead of blocking indefinitely.
     */
    @Test
    void gracefulShutdownTimeoutExpiresAndProceedsWithTeardown() {
        ServiceGraph graph = new ServiceGraph();
        graph.setGracefulShutdownConfig(false, null, true, Duration.ofMillis(60));
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        ServiceNode svc = createStandardGraph(graph, makeAction(node -> {
            node.onGracefulShutdownAsync(ctxt -> {
                events.add("shutdownStarted");
                // Intentionally do not invoke ctxt.done() to trigger timeout
            });
            node.onStop(() -> events.add("stop"));
            node.startComplete();
        }));

        graph.start();
        long startNanos = System.nanoTime();
        graph.stop();
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        assertThat(events).containsExactly("shutdownStarted", "stop");
        assertThat(svc.state()).isEqualTo(ServiceNode.S_STOPPED);
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(50);
    }

    /**
     * Verify that unhandled exceptions thrown by pre-shutdown or shutdown handlers
     * are caught and logged without aborting subsequent graceful phases or service teardown.
     */
    @Test
    void gracefulShutdownHandlerExceptionHandledGracefully() {
        ServiceGraph graph = new ServiceGraph();
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        createStandardGraph(graph, makeAction(node -> {
            node.onGracefulPreShutdown(() -> {
                events.add("preShutdownThrow");
                throw new RuntimeException("Deliberate pre-shutdown exception");
            });
            node.onGracefulShutdown(() -> {
                events.add("shutdownThrow");
                throw new RuntimeException("Deliberate shutdown exception");
            });
            node.onStop(() -> events.add("stop"));
            node.startComplete();
        }));

        graph.start();
        graph.stop();

        assertThat(events).containsExactly("preShutdownThrow", "shutdownThrow", "stop");
    }

    /**
     * Verify that invoking {@link io.quarkus.core.AsyncGracefulShutdownContext#done()}
     * multiple times from a single handler is idempotent and safe.
     */
    @Test
    void gracefulShutdownContextDoneIsIdempotent() {
        ServiceGraph graph = new ServiceGraph();
        AtomicInteger doneCount = new AtomicInteger();
        createStandardGraph(graph, makeAction(node -> {
            node.onGracefulShutdownAsync(ctxt -> {
                ctxt.done();
                ctxt.done();
                ctxt.done();
                doneCount.incrementAndGet();
            });
            node.startComplete();
        }));

        graph.start();
        graph.stop();

        assertThat(doneCount.get()).isEqualTo(1);
    }

    /**
     * Verify that when a multi-threaded executor is configured on the graph,
     * graceful shutdown handlers execute concurrently.
     */
    @Test
    void gracefulShutdownRunsConcurrentlyOnParallelExecutor() {
        ServiceGraph graph = new ServiceGraph();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            graph.setExecutor(executor);
            CountDownLatch syncLatch = new CountDownLatch(4);
            AtomicInteger concurrentCount = new AtomicInteger();
            AtomicInteger maxConcurrent = new AtomicInteger();

            createStandardGraph(graph, makeAction(node -> {
                for (int i = 0; i < 4; i++) {
                    node.onGracefulShutdownAsync(ctxt -> {
                        int current = concurrentCount.incrementAndGet();
                        maxConcurrent.accumulateAndGet(current, Math::max);
                        syncLatch.countDown();
                        try {
                            syncLatch.await(2, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            concurrentCount.decrementAndGet();
                            ctxt.done();
                        }
                    });
                }
                node.startComplete();
            }));

            graph.start();
            graph.stop();

            assertThat(maxConcurrent.get()).isGreaterThan(1);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Verify that if the thread executing {@link ServiceGraph#stop()} is interrupted
     * while waiting during the delay phase, the sleep terminates early, the thread's
     * interrupted status is restored, and remaining teardown completes.
     */
    @Test
    void gracefulShutdownInterruptedDuringDelayRestoresInterruptFlag() {
        ServiceGraph graph = new ServiceGraph();
        graph.setGracefulShutdownConfig(true, Duration.ofSeconds(10), false, null);
        AtomicBoolean stopped = new AtomicBoolean();
        createStandardGraph(graph, makeAction(node -> {
            node.onGracefulPreShutdown(() -> {
                // Set the interrupt status right before the delay phase sleep begins
                Thread.currentThread().interrupt();
            });
            node.onStop(() -> stopped.set(true));
            node.startComplete();
        }));

        graph.start();
        try {
            graph.stop();
            assertThat(Thread.interrupted()).isTrue();
            assertThat(stopped).isTrue();
        } finally {
            Thread.interrupted(); // Clear interrupt flag if assertion fails
        }
    }

    /**
     * Verify that legacy {@link io.quarkus.runtime.shutdown.ShutdownListener} instances
     * registered via {@link io.quarkus.runtime.shutdown.ShutdownRecorder} and legacy
     * configuration are bridged into graceful shutdown when explicit configuration is omitted.
     */
    @Test
    @SuppressWarnings("deprecation")
    void gracefulShutdownLegacyListenersAndConfigBridged() {
        ServiceGraph graph = new ServiceGraph();
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        ShutdownListener listener = new ShutdownListener() {
            @Override
            public void preShutdown(ShutdownNotification notification) {
                events.add("legacyPreShutdown");
                notification.done();
            }

            @Override
            public void shutdown(ShutdownNotification notification) {
                events.add("legacyShutdown");
                notification.done();
            }
        };
        ShutdownConfig config = new ShutdownConfig() {
            @Override
            public Optional<Duration> timeout() {
                return Optional.of(Duration.ofMillis(100));
            }

            @Override
            public Optional<Duration> delay() {
                return Optional.empty();
            }

            @Override
            public boolean isTimeoutEnabled() {
                return true;
            }

            @Override
            public boolean isDelayEnabled() {
                return false;
            }
        };
        ShutdownRecorder recorder = new ShutdownRecorder(new RuntimeValue<>(config));
        recorder.setListeners(List.of(listener), true);
        try {
            createStandardGraph(graph, MH_VOID);
            graph.start();
            graph.stop();
            assertThat(events).containsExactly("legacyPreShutdown", "legacyShutdown");
        } finally {
            recorder.setListeners(null, false);
        }
    }

    /**
     * Verify that calling {@link ServiceGraph#stop()} on an empty graph (with no sentinels
     * or services configured) returns immediately without throwing an exception.
     */
    @Test
    void gracefulShutdownEmptyGraphStopsWithoutError() {
        ServiceGraph graph = new ServiceGraph();
        graph.stop();
    }

    /**
     * Verify that calling {@link ServiceGraph#stop()} when no graceful handlers are
     * registered proceeds directly to service node stop cascade.
     */
    @Test
    void gracefulShutdownEmptyHandlersProceedsImmediately() {
        ServiceGraph graph = new ServiceGraph();
        AtomicBoolean stopped = new AtomicBoolean();
        createStandardGraph(graph, makeAction(node -> {
            node.onStop(() -> stopped.set(true));
            node.startComplete();
        }));

        graph.start();
        graph.stop();

        assertThat(stopped).isTrue();
    }

    // ═══════════════════════════════════════════════
    // NodeShutdownContext
    // ═══════════════════════════════════════════════

    /**
     * Verify that {@link NodeShutdownContext} executes tasks in LIFO order.
     */
    @Test
    void nodeShutdownContextRunsTasksInLifoOrder() {
        NodeShutdownContext sc = new NodeShutdownContext();
        List<String> order = new ArrayList<>();
        sc.addShutdownTask(() -> order.add("first"));
        sc.addShutdownTask(() -> order.add("second"));
        sc.addShutdownTask(() -> order.add("third"));

        sc.run();

        assertThat(order).containsExactly("third", "second", "first");
    }

    /**
     * Verify that {@code addLastShutdownTask} registers tasks with the global collector.
     */
    @Test
    void lastShutdownTasksDelegatedToGlobalCollector() {
        NodeShutdownContext scStatic = new NodeShutdownContext(true);
        NodeShutdownContext scRuntime = new NodeShutdownContext(false);
        List<String> order = new ArrayList<>();
        scStatic.addShutdownTask(() -> order.add("normalStatic"));
        scStatic.addLastShutdownTask(() -> order.add("lastStatic"));
        scRuntime.addShutdownTask(() -> order.add("normalRuntime"));
        scRuntime.addLastShutdownTask(() -> order.add("lastRuntime"));

        // normal tasks run on the node
        scStatic.run();
        scRuntime.run();
        // "last" tasks are collected globally and run separately
        LastShutdownTasks.runRuntime();
        LastShutdownTasks.runStatic();

        assertThat(order).containsExactly("normalStatic", "normalRuntime", "lastRuntime", "lastStatic");
    }

    /**
     * Verify that {@link NodeShutdownContext} clears registered tasks after running.
     */
    @Test
    void nodeShutdownContextClearsAfterRun() {
        NodeShutdownContext sc = new NodeShutdownContext();
        List<String> order = new ArrayList<>();
        sc.addShutdownTask(() -> order.add("task"));

        sc.run();
        assertThat(order).hasSize(1);

        // second run should be empty
        sc.run();
        assertThat(order).hasSize(1);
    }

    // ═══════════════════════════════════════════════
    // Executor swap
    // ═══════════════════════════════════════════════

    /**
     * Verify that {@link ServiceGraph#setExecutor(Executor)} swaps the executor
     * and returns the previous executor instance.
     */
    @Test
    void executorSwapReturnsOldExecutor() {
        ServiceGraph graph = new ServiceGraph();
        Executor original = graph.executor();

        Executor custom = Runnable::run;
        Executor returned = graph.setExecutor(custom);

        assertThat(returned).isSameAs(original);
        assertThat(graph.executor()).isSameAs(custom);
    }

    // ═══════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════

    /**
     * Helper to build and wire a standard three-node graph consisting of a top sentinel,
     * a single service node, and a bottom sentinel.
     *
     * @param graph the service graph
     * @param svcAction the action method handle for the service node
     * @return the created service node
     */
    private static ServiceNode createStandardGraph(ServiceGraph graph, MethodHandle svcAction) {
        ServiceNode top = new ServiceNode("top", MH_TOP, graph, 1);
        ServiceNode svc = new ServiceNode("svc", svcAction, graph, 1, top);
        ServiceNode bottom = new ServiceNode("bottom", MH_BOTTOM, graph, 0, svc);
        graph.setTop(top);
        graph.setBottom(bottom);
        return svc;
    }

    /**
     * Create an action handle that delegates execution to the specified consumer.
     *
     * @param consumer the consumer action to run when the node starts
     * @return the method handle for the action
     */
    private static MethodHandle makeAction(Consumer<ServiceNode> consumer) {
        try {
            MethodHandle base = MethodHandles.lookup().findStatic(
                    ServiceGraphTest.class, "consumerAction",
                    MethodType.methodType(void.class, ServiceNode.class, Consumer.class));
            return MethodHandles.insertArguments(base, 1, consumer);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delegate method invoked by method handles created via {@link #makeAction(Consumer)}.
     *
     * @param node the service node
     * @param consumer the consumer action to execute
     */
    static void consumerAction(ServiceNode node, Consumer<ServiceNode> consumer) {
        consumer.accept(node);
    }

    /**
     * Create an action handle that completes as void and registers a stop handler
     * that records the node name into the given list.
     *
     * @param name the node name
     * @param stopOrder the list to record stop order
     * @return the method handle
     */
    private static MethodHandle makeStopTracker(String name, List<String> stopOrder) {
        // we can't easily create a closure MethodHandle, so use a workaround:
        // store the stop order list in a static field and use the node name
        // Actually, let's use a simple approach: the action registers the stop handler
        try {
            MethodHandle base = MethodHandles.lookup().findStatic(
                    ServiceGraphTest.class, "stopTrackingAction",
                    MethodType.methodType(void.class, ServiceNode.class, List.class));
            return MethodHandles.insertArguments(base, 1, stopOrder);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Action that registers a stop handler to record the node name in the given list,
     * then completes as void.
     *
     * @param node the service node
     * @param stopOrder the list to record stop order
     */
    static void stopTrackingAction(ServiceNode node, List<String> stopOrder) {
        node.onStop(() -> stopOrder.add(node.name()));
        node.startComplete();
    }
}
