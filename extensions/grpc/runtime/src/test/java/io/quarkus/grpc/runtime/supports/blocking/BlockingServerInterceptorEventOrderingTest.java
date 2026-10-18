package io.quarkus.grpc.runtime.supports.blocking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Vertx;

/**
 * Verifies that {@link BlockingServerInterceptor} does not deliver {@code onHalfClose}
 * to the underlying unary listener before {@code onMessage}, even when the wire-level
 * race condition causes the replay listener to receive {@code onHalfClose} first.
 * <p>
 * Background: {@code next.startCall} (grpc-stub's {@code UnaryServerCallHandler.startCall})
 * is the call site that grants flow-control budget via {@code call.request(2)}. The blocking
 * interceptor defers that call onto a worker / virtual thread executor. If that executor is
 * delayed (cold pool, GC, executor backlog), the gRPC framework can deliver {@code onHalfClose}
 * (which does not require budget — it rides END_STREAM) to the replay listener BEFORE the
 * deframer is allowed to deliver {@code onMessage} (which is gated on {@code call.request(N)}).
 * Without protection, the replay listener queues events in arrival order and replays them
 * FIFO, so the real {@code UnaryServerCallListener} sees {@code onHalfClose} with
 * {@code request == null} and closes the call with
 * {@code Status.INTERNAL.withDescription("Half-closed without a request")}.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class BlockingServerInterceptorEventOrderingTest {

    Vertx vertx;
    InjectableContext.ContextState contextState;
    ManagedContext requestContext;
    /** Single-slot executor we drive manually so we can inject the race deterministically. */
    BlockingQueue<Runnable> deferred;
    Executor controllableVirtualExecutor;

    @BeforeEach
    void setup() {
        vertx = Vertx.vertx();
        contextState = mock(InjectableContext.ContextState.class);
        requestContext = mock(ManagedContext.class);
        when(requestContext.getState()).thenReturn(contextState);
        deferred = new ArrayBlockingQueue<>(4);
        controllableVirtualExecutor = deferred::add;
    }

    @AfterEach
    void teardown() {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().orTimeout(5, TimeUnit.SECONDS).join();
        }
    }

    /**
     * Reproduces the production race for the {@code @RunOnVirtualThread} (virtual-thread) path.
     * The replay listener receives {@code onHalfClose} before {@code onMessage} (because budget
     * was deferred), then the virtual-thread executor finally runs {@code next.startCall} and
     * {@code setDelegate}. The real listener MUST observe {@code onMessage} before
     * {@code onHalfClose}.
     */
    @Test
    @Timeout(10)
    void virtualThreadPath_deliversOnMessageBeforeOnHalfClose_evenIfEnqueuedInReverse() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.emptyList(),
                Collections.singletonList("unary"));

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/unary");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.UNARY);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();

        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        // Simulate the framework delivering events to the replay listener BEFORE the virtual-thread
        // executor has run next.startCall. onHalfClose arrives first because END_STREAM does not
        // require flow-control budget; onMessage was held in the deframer until budget was granted.
        replayListener.onHalfClose();
        replayListener.onMessage("hi");

        // Now run the deferred next.startCall. This grants budget (call.request(2)) and installs
        // the real listener as the replay delegate, draining the queued events.
        runAllDeferredTasks();

        // The real listener must see onMessage first, then onHalfClose. If the bug is present,
        // the recorded order will be [onHalfClose, onMessage], which is what triggers
        // UnaryServerCallListener to close the call with INTERNAL: Half-closed without a request.
        next.awaitEvents(2);
        assertThat(next.events)
                .as("real listener must receive onMessage before onHalfClose to satisfy "
                        + "UnaryServerCallListener's request-not-null invariant")
                .containsExactly("onMessage:hi", "onHalfClose");
    }

    /**
     * Same race on the {@code @Blocking} (Vert.x worker) path. Uses
     * {@code vertx.executeBlocking} which we cannot easily control, so we rely on the fact
     * that {@code scheduleOrEnqueue} enqueues both events synchronously while the worker task
     * is still in-flight, then we await replay completion via the recording handler's latch.
     */
    @Test
    @Timeout(10)
    void blockingPath_deliversOnMessageBeforeOnHalfClose_evenIfEnqueuedInReverse() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.singletonList("unary"),
                Collections.emptyList());

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/unary");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.UNARY);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        // Hold up next.startCall just long enough that we can enqueue events before setDelegate runs.
        next.startCallGate.set(true);

        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        // Enqueue events in the buggy order while the worker thread is still blocked inside
        // next.startCall (i.e. before setDelegate has been invoked).
        replayListener.onHalfClose();
        replayListener.onMessage("hi");

        // Release next.startCall so the worker thread can finish, setDelegate runs, and the
        // queue is drained.
        next.startCallGate.set(false);
        synchronized (next.startCallGate) {
            next.startCallGate.notifyAll();
        }

        next.awaitEvents(2);
        assertThat(next.events)
                .as("real listener must receive onMessage before onHalfClose to satisfy "
                        + "UnaryServerCallListener's request-not-null invariant")
                .containsExactly("onMessage:hi", "onHalfClose");
    }

    /**
     * Happy-path: events arrive in the correct order on the virtual-thread path.
     * The reordering logic must not disturb a queue that is already correctly ordered.
     */
    @Test
    @Timeout(10)
    void virtualThreadPath_preservesCorrectOrderWhenEventsArriveInOrder() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.emptyList(),
                Collections.singletonList("unary"));

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/unary");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.UNARY);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        // Events arrive in the correct order: onMessage before onHalfClose.
        replayListener.onMessage("hi");
        replayListener.onHalfClose();

        runAllDeferredTasks();

        next.awaitEvents(2);
        assertThat(next.events)
                .as("correctly ordered events must not be reordered")
                .containsExactly("onMessage:hi", "onHalfClose");
    }

    /**
     * Happy-path: events arrive in the correct order on the blocking path.
     */
    @Test
    @Timeout(10)
    void blockingPath_preservesCorrectOrderWhenEventsArriveInOrder() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.singletonList("unary"),
                Collections.emptyList());

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/unary");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.UNARY);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        next.startCallGate.set(true);

        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        // Events arrive in the correct order while the worker thread is still blocked.
        replayListener.onMessage("hi");
        replayListener.onHalfClose();

        next.startCallGate.set(false);
        synchronized (next.startCallGate) {
            next.startCallGate.notifyAll();
        }

        next.awaitEvents(2);
        assertThat(next.events)
                .as("correctly ordered events must not be reordered")
                .containsExactly("onMessage:hi", "onHalfClose");
    }

    /**
     * Verifies that when multiple messages are queued after {@code onHalfClose}, all of them
     * are promoted before the half-close and their relative order is preserved.
     * Queue before fix: [onHalfClose, onMessage:first, onMessage:second]
     * Expected after fix: [onMessage:first, onMessage:second, onHalfClose]
     */
    @Test
    @Timeout(10)
    void virtualThreadPath_promotesMultipleMessagesBeforeHalfClose() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.emptyList(),
                Collections.singletonList("unary"));

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/unary");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.UNARY);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        // All three arrive before the deferred executor runs.
        replayListener.onHalfClose();
        replayListener.onMessage("first");
        replayListener.onMessage("second");

        runAllDeferredTasks();

        next.awaitEvents(3);
        assertThat(next.events)
                .as("both messages must be promoted before onHalfClose, preserving their relative order")
                .containsExactly("onMessage:first", "onMessage:second", "onHalfClose");
    }

    /**
     * Same multi-message promotion on the blocking ({@code @Blocking}) path.
     */
    @Test
    @Timeout(10)
    void blockingPath_promotesMultipleMessagesBeforeHalfClose() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.singletonList("unary"),
                Collections.emptyList());

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/unary");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.UNARY);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        next.startCallGate.set(true);

        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        replayListener.onHalfClose();
        replayListener.onMessage("first");
        replayListener.onMessage("second");

        next.startCallGate.set(false);
        synchronized (next.startCallGate) {
            next.startCallGate.notifyAll();
        }

        next.awaitEvents(3);
        assertThat(next.events)
                .as("both messages must be promoted before onHalfClose, preserving their relative order")
                .containsExactly("onMessage:first", "onMessage:second", "onHalfClose");
    }

    /**
     * {@code onHalfClose} alone is queued before the deferred executor runs; {@code onMessage} arrives only
     * after {@code setDelegate}.
     */
    @Test
    @Timeout(10)
    void virtualThreadPath_deliversOnMessageBeforeOnHalfClose_whenMessageArrivesAfterSetDelegate() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.emptyList(),
                Collections.singletonList("unary"));

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/unary");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.UNARY);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        replayListener.onHalfClose();
        runAllDeferredTasks();
        replayListener.onMessage("hi");
        runAllDeferredTasks();

        next.awaitEvents(2);
        assertThat(next.events)
                .as("onMessage must be delivered after setDelegate even if onHalfClose was queued first")
                .containsExactly("onMessage:hi", "onHalfClose");
    }

    /**
     * Same deferred-message race on the {@code @Blocking} path.
     */
    @Test
    @Timeout(10)
    void blockingPath_deliversOnMessageBeforeOnHalfClose_whenMessageArrivesAfterSetDelegate() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.singletonList("unary"),
                Collections.emptyList());

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/unary");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.UNARY);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        next.startCallGate.set(true);

        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        replayListener.onHalfClose();
        next.startCallGate.set(false);
        synchronized (next.startCallGate) {
            next.startCallGate.notifyAll();
        }
        replayListener.onMessage("hi");

        next.awaitEvents(2);
        assertThat(next.events)
                .as("onMessage must be delivered after setDelegate even if onHalfClose was queued first")
                .containsExactly("onMessage:hi", "onHalfClose");
    }

    @Test
    @Timeout(10)
    void serverStreaming_virtualThreadPath_defersHalfCloseUntilRequestMessage() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.emptyList(),
                Collections.singletonList("serverStreaming"));

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/serverStreaming");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.SERVER_STREAMING);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        replayListener.onHalfClose();
        runAllDeferredTasks();
        assertThat(next.events).isEmpty();

        replayListener.onMessage("hi");
        runAllDeferredTasks();

        next.awaitEvents(2);
        assertThat(next.events).containsExactly("onMessage:hi", "onHalfClose");
    }

    @Test
    @Timeout(10)
    void clientStreaming_virtualThreadPath_promotesMultipleMessagesBeforeHalfClose() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.emptyList(),
                Collections.singletonList("clientStreaming"));

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/clientStreaming");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.CLIENT_STREAMING);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        replayListener.onHalfClose();
        replayListener.onMessage("first");
        replayListener.onMessage("second");
        runAllDeferredTasks();

        next.awaitEvents(3);
        assertThat(next.events).containsExactly("onMessage:first", "onMessage:second", "onHalfClose");
    }

    @Test
    @Timeout(10)
    void clientStreaming_virtualThreadPath_allowsHalfCloseWithoutRequestMessage() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.emptyList(),
                Collections.singletonList("clientStreaming"));

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/clientStreaming");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.CLIENT_STREAMING);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);

        replayListener.onHalfClose();
        runAllDeferredTasks();

        next.awaitEvents(1);
        assertThat(next.events).containsExactly("onHalfClose");
    }

    @Test
    @Timeout(10)
    void virtualThreadPath_messageReceivedSetUnderLockBeforeSetDelegateDrains() throws Exception {
        BlockingServerInterceptor interceptor = newInterceptor(Collections.emptyList(),
                Collections.singletonList("unary"));

        ServerCall serverCall = mock(ServerCall.class);
        MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/unary");
        when(methodDescriptor.getType()).thenReturn(MethodDescriptor.MethodType.UNARY);
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        RecordingServerCallHandler next = new RecordingServerCallHandler();
        next.blockStartCallUntilMessageReceived.set(true);

        ServerCall.Listener replayListener = interceptor.interceptCall(serverCall, new Metadata(), next);
        replayListener.onHalfClose();

        Thread executorThread = new Thread(() -> {
            Runnable task = deferred.poll();
            if (task != null) {
                task.run();
            }
        });
        executorThread.start();
        executorThread.join(2000);

        assertThat(next.events).isEmpty();

        replayListener.onMessage("hi");
        next.blockStartCallUntilMessageReceived.set(false);
        synchronized (next.blockStartCallUntilMessageReceived) {
            next.blockStartCallUntilMessageReceived.notifyAll();
        }
        executorThread.join(2000);
        runAllDeferredTasks();

        next.awaitEvents(2);
        assertThat(next.events).containsExactly("onMessage:hi", "onHalfClose");
    }

    private BlockingServerInterceptor newInterceptor(List<String> blocking, List<String> virtual) {
        return new BlockingServerInterceptor(vertx, blocking, virtual, controllableVirtualExecutor, false) {
            @Override
            protected boolean isExecutable() {
                return true;
            }

            @Override
            protected ManagedContext getRequestContext() {
                return requestContext;
            }
        };
    }

    private void runAllDeferredTasks() {
        Runnable task;
        while ((task = deferred.poll()) != null) {
            task.run();
        }
    }

    /**
     * A {@link ServerCallHandler} whose returned listener records every event in order on a
     * shared list, and whose {@code startCall} can be gated to simulate a slow executor.
     */
    static class RecordingServerCallHandler implements ServerCallHandler {

        final List<String> events = new CopyOnWriteArrayList<>();
        final java.util.concurrent.atomic.AtomicBoolean startCallGate = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.atomic.AtomicBoolean blockStartCallUntilMessageReceived = new java.util.concurrent.atomic.AtomicBoolean(
                false);

        @Override
        public ServerCall.Listener startCall(ServerCall serverCall, Metadata metadata) {
            // If gated, block this thread until released. Used to widen the race window for the
            // @Blocking path which uses vertx.executeBlocking (an executor we don't directly drive).
            synchronized (startCallGate) {
                while (startCallGate.get()) {
                    try {
                        startCallGate.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
            synchronized (blockStartCallUntilMessageReceived) {
                while (blockStartCallUntilMessageReceived.get()) {
                    try {
                        blockStartCallUntilMessageReceived.wait(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
            return new ServerCall.Listener() {
                @Override
                public void onMessage(Object message) {
                    events.add("onMessage:" + message);
                }

                @Override
                public void onHalfClose() {
                    events.add("onHalfClose");
                }

                @Override
                public void onCancel() {
                    events.add("onCancel");
                }

                @Override
                public void onComplete() {
                    events.add("onComplete");
                }

                @Override
                public void onReady() {
                    events.add("onReady");
                }
            };
        }

        void awaitEvents(int count) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (events.size() < count && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            if (events.size() < count) {
                throw new AssertionError("Expected " + count + " events, got " + events);
            }
        }
    }

}
