package io.quarkus.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ServiceNode} and {@link ServiceGraph} lifecycle.
 * <p>
 * Tests verify the state machine transitions, dependency satisfaction,
 * failure cascade, stop cascade, and executor swap behaviors.
 */
class ServiceGraphTest {

    // ═══════════════════════════════════════════════
    // Action handles for test services
    // ═══════════════════════════════════════════════

    /** Action that completes with a string value. */
    static void typedAction(ServiceNode node) {
        node.startComplete("value-from-" + node.name());
    }

    /** Action that completes as void. */
    static void voidAction(ServiceNode node) {
        node.startComplete();
    }

    /** Action that fails immediately. */
    static void failingAction(ServiceNode node) {
        node.startFailed(new RuntimeException("deliberate failure in " + node.name()));
    }

    /** Action that reads dependency values. */
    static void depReadingAction(ServiceNode node) {
        Object dep0 = node.dependencyValue(0);
        node.startComplete("got-" + dep0);
    }

    /** Top sentinel action: registers stop-done handler, then completes. */
    static void topSentinel(ServiceNode node) {
        node.onStop(() -> node.graph().signalStopDone());
        node.startComplete();
    }

    /** Bottom sentinel action: signals start-done, then completes. */
    static void bottomSentinel(ServiceNode node) {
        node.graph().signalStartDone();
        node.startComplete();
    }

    private static final MethodHandle MH_TYPED;
    private static final MethodHandle MH_VOID;
    private static final MethodHandle MH_FAILING;
    private static final MethodHandle MH_DEP_READING;
    private static final MethodHandle MH_TOP;
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

        assertThat(a.state()).isEqualTo(ServiceNode.S_FAILED);
        assertThat(b.state()).isEqualTo(ServiceNode.S_CANCELED);
        assertThat(a.failure()).isNotNull();
        assertThat(a.failure().getMessage()).contains("deliberate failure");
    }

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

        assertThat(b.state()).isEqualTo(ServiceNode.S_FAILED);
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
    // NodeShutdownContext
    // ═══════════════════════════════════════════════

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

    @Test
    void lastShutdownTasksDelegatedToGlobalCollector() {
        NodeShutdownContext sc = new NodeShutdownContext();
        List<String> order = new ArrayList<>();
        sc.addShutdownTask(() -> order.add("normal"));
        sc.addLastShutdownTask(() -> order.add("last"));

        // normal tasks run on the node
        sc.run();
        // "last" tasks are collected globally and run separately
        LastShutdownTasks.run();

        assertThat(order).containsExactly("normal", "last");
    }

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
     * Create an action handle that completes as void and registers a stop handler
     * that records the node name into the given list.
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

    static void stopTrackingAction(ServiceNode node, List<String> stopOrder) {
        node.onStop(() -> stopOrder.add(node.name()));
        node.startComplete();
    }
}
