package io.quarkus.grpc.runtime.supports.blocking;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.inject.spi.Prioritized;

import org.jboss.logging.Logger;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.quarkus.grpc.runtime.Interceptors;
import io.vertx.core.Vertx;

/**
 * gRPC Server interceptor offloading the execution of the gRPC method on a worker thread if the method is annotated
 * with {@link io.smallrye.common.annotation.Blocking}.
 * <p>
 * For non-annotated methods, the interceptor acts as a pass-through.
 */
public class BlockingServerInterceptor implements ServerInterceptor, Function<String, Boolean>, Prioritized {
    private static final Logger log = Logger.getLogger(BlockingServerInterceptor.class);

    // Reserved keywords, based on the jls, see:
    // https://github.com/grpc/grpc-java/blob/master/compiler/src/java_plugin/cpp/java_generator.cpp#L90
    private static final Set<String> GRPC_JAVA_RESERVED_KEYWORDS = Set.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while",
            "true",
            "false");

    private final Vertx vertx;
    private final Set<String> blockingMethods;
    private final Set<String> virtualMethods;
    private final Map<String, Boolean> blockingCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> virtualCache = new ConcurrentHashMap<>();
    private final boolean devMode;
    private final Executor virtualThreadExecutor;

    public BlockingServerInterceptor(Vertx vertx, List<String> blockingMethods, List<String> virtualMethods,
            Executor virtualThreadExecutor, boolean devMode) {
        this.vertx = vertx;
        this.blockingMethods = new HashSet<>();
        this.virtualMethods = new HashSet<>();
        this.devMode = devMode;
        if (blockingMethods != null) {
            for (String method : blockingMethods) {
                this.blockingMethods.add(method.toLowerCase());
            }
        }

        if (virtualMethods != null) {
            for (String method : virtualMethods) {
                this.virtualMethods.add(method.toLowerCase());
            }
        }
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Override
    public Boolean apply(String name) {
        String methodName = name.substring(name.lastIndexOf("/") + 1);
        return blockingMethods.contains(toLowerCaseBeanSpec(methodName));
    }

    public Boolean applyVirtual(String name) {
        String methodName = name.substring(name.lastIndexOf("/") + 1);
        return virtualMethods.contains(toLowerCaseBeanSpec(methodName));
    }

    private String toLowerCaseBeanSpec(String name) {

        // Methods cannot always be lowercased for comparison.
        // - gRPC allows using method names which normally would not work in java because of reserved keywords.
        // - Underscores are removed.

        String lowerBeanSpec = name.toLowerCase().replace("_", "");
        return GRPC_JAVA_RESERVED_KEYWORDS.contains(lowerBeanSpec) ? lowerBeanSpec + "_" : lowerBeanSpec;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // We need to check if the method is annotated with @Blocking.
        // Unfortunately, we can't have the Java method object, we can only have the gRPC full method name.
        // We extract the method name, and check if the name is in the list.
        // This makes the following assumptions:
        // 1. the code generator does not change the method name (which makes sense)
        // 2. the method name is unique, which is a constraint of gRPC

        // For performance purpose, we execute the lookup only once
        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        boolean isBlocking = blockingCache.computeIfAbsent(fullMethodName, this);
        boolean isVirtual = virtualCache.computeIfAbsent(fullMethodName, this::applyVirtual);

        if (isVirtual) {
            final ManagedContext requestContext = getRequestContext();
            // context should always be active here
            // it is initialized by io.quarkus.grpc.runtime.supports.context.GrpcRequestContextGrpcInterceptor
            // that should always be called before this interceptor
            ContextState state = requestContext.getState();
            VirtualReplayListener<ReqT> replay = new VirtualReplayListener<>(state);
            virtualThreadExecutor.execute(() -> {
                ServerCall.Listener<ReqT> listener;
                try {
                    requestContext.activate(state);
                    listener = next.startCall(call, headers);
                } finally {
                    requestContext.deactivate();
                }
                replay.setDelegate(listener);
            });
            return replay;
        } else if (isBlocking) {
            final ManagedContext requestContext = getRequestContext();
            // context should always be active here
            // it is initialized by io.quarkus.grpc.runtime.supports.context.GrpcRequestContextGrpcInterceptor
            // that should always be called before this interceptor
            ContextState state = requestContext.getState();
            ReplayListener<ReqT> replay = new ReplayListener<>(state);
            vertx.executeBlocking(() -> {
                ServerCall.Listener<ReqT> listener;
                try {
                    requestContext.activate(state);
                    listener = next.startCall(call, headers);
                } finally {
                    requestContext.deactivate();
                }
                return listener;
            }, false)
                    .onComplete(event -> replay.setDelegate(event.result()));

            return replay;
        } else {
            return next.startCall(call, headers);
        }
    }

    @Override
    public int getPriority() {
        return Interceptors.BLOCKING_HANDLER;
    }

    /**
     * Stores the incoming events until the listener is injected.
     * When injected, replay the events.
     * <p>
     * Note that event must be executed in order, explaining why incomingEvents
     * are executed sequentially
     */
    private class ReplayListener<ReqT> extends ServerCall.Listener<ReqT> {
        private final InjectableContext.ContextState requestContextState;
        private final Context grpcContext;

        // exclusive to event loop context
        private volatile ServerCall.Listener<ReqT> delegate;
        // Guarded by its own monitor. Events queued before setDelegate runs may arrive
        // out of order versus how the underlying UnaryServerCallListener expects them
        // (e.g. onHalfClose before onMessage when call.request(N) was deferred — see
        // setDelegate). The deque is reordered once at delegate-injection time to
        // preserve the request-before-half-close invariant before draining.
        private final Deque<ReplayEvent<ReqT>> incomingEvents = new ArrayDeque<>();
        private volatile boolean isConsumingFromIncomingEvents;

        private ReplayListener(InjectableContext.ContextState requestContextState) {
            this.requestContextState = requestContextState;
            this.grpcContext = Context.current();
        }

        /**
         * Must be called from within the event loop context
         * If there are deferred events will start executing them in the shared worker context
         *
         * @param delegate the original
         */
        void setDelegate(ServerCall.Listener<ReqT> delegate) {
            ReplayEvent<ReqT> first = null;
            synchronized (incomingEvents) {
                this.delegate = delegate;
                if (!this.isConsumingFromIncomingEvents) {
                    reorderForRequestBeforeHalfClose(incomingEvents);
                    first = incomingEvents.poll();
                }
            }
            if (first != null) {
                executeBlockingWithRequestContext(first.action);
            }
        }

        private void scheduleOrEnqueue(ReplayEvent<ReqT> event) {
            boolean executeNow = false;
            synchronized (incomingEvents) {
                if (this.delegate != null && !this.isConsumingFromIncomingEvents && incomingEvents.isEmpty()) {
                    executeNow = true;
                } else {
                    incomingEvents.add(event);
                }
            }
            if (executeNow) {
                executeBlockingWithRequestContext(event.action);
            }
        }

        /**
         * Will execute the consumer in a worker context
         * Once complete will enqueue the next consumer for execution.
         * This method guarantees ordered execution per request.
         *
         * @param consumer the original
         */
        private void executeBlockingWithRequestContext(Consumer<ServerCall.Listener<ReqT>> consumer) {
            final Context grpcContext = Context.current();
            Callable<Void> blockingHandler = new BlockingExecutionHandler<>(consumer, grpcContext, delegate,
                    requestContextState, getRequestContext(), this);

            if (!isExecutable()) {
                log.warn("Not executable, already shutdown? Ignoring execution ...");
                return;
            }

            if (devMode) {
                blockingHandler = new DevModeBlockingExecutionHandler(Thread.currentThread().getContextClassLoader(),
                        blockingHandler);
            }
            // Written outside the lock intentionally: this method is only ever called from
            // the event loop thread (directly or via the onComplete handler which is also
            // on the event loop). gRPC guarantees sequential event delivery on the event
            // loop, so no concurrent scheduleOrEnqueue call can race with this true-write.
            // The transition back to false is done under the lock because it happens on the
            // worker thread (where a concurrent scheduleOrEnqueue from the event loop could
            // otherwise observe a stale false and bypass the queue). The volatile keyword
            // ensures both transitions are immediately visible across threads.
            this.isConsumingFromIncomingEvents = true;
            vertx.executeBlocking(blockingHandler, false).onComplete(p -> {
                ReplayEvent<ReqT> next;
                synchronized (incomingEvents) {
                    next = incomingEvents.poll();
                    if (next == null) {
                        this.isConsumingFromIncomingEvents = false;
                    }
                }
                if (next != null) {
                    executeBlockingWithRequestContext(next.action);
                }
            });
        }

        @Override
        public void onMessage(ReqT message) {
            scheduleOrEnqueue(new ReplayEvent<>(EventKind.MESSAGE, t -> t.onMessage(message)));
        }

        @Override
        public void onHalfClose() {
            scheduleOrEnqueue(new ReplayEvent<>(EventKind.HALF_CLOSE, ServerCall.Listener::onHalfClose));
        }

        @Override
        public void onCancel() {
            scheduleOrEnqueue(new ReplayEvent<>(EventKind.OTHER, ServerCall.Listener::onCancel));
        }

        @Override
        public void onComplete() {
            scheduleOrEnqueue(new ReplayEvent<>(EventKind.OTHER, ServerCall.Listener::onComplete));
        }

        @Override
        public void onReady() {
            scheduleOrEnqueue(new ReplayEvent<>(EventKind.OTHER, ServerCall.Listener::onReady));
        }
    }

    /**
     * Stores the incoming events until the listener is injected.
     * When injected, replay the events.
     * <p>
     * Note that event must be executed in order, explaining why incomingEvents
     * are executed sequentially
     * <p>
     * This replay listener is only used for virtual threads.
     */
    private class VirtualReplayListener<ReqT> extends ServerCall.Listener<ReqT> {
        private final InjectableContext.ContextState requestContextState;

        // exclusive to event loop context
        private ServerCall.Listener<ReqT> delegate;
        // Guarded by its own monitor. See ReplayListener#incomingEvents for the
        // rationale on why this is reordered before draining.
        private final Deque<ReplayEvent<ReqT>> incomingEvents = new ArrayDeque<>();
        private volatile boolean isConsumingFromIncomingEvents = false;

        private VirtualReplayListener(InjectableContext.ContextState requestContextState) {
            this.requestContextState = requestContextState;
        }

        /**
         * Must be called from within the event loop context
         * If there are deferred events will start executing them in the shared worker context
         *
         * @param delegate the original
         */
        void setDelegate(ServerCall.Listener<ReqT> delegate) {
            ReplayEvent<ReqT> first = null;
            synchronized (incomingEvents) {
                this.delegate = delegate;
                if (!this.isConsumingFromIncomingEvents) {
                    reorderForRequestBeforeHalfClose(incomingEvents);
                    first = incomingEvents.poll();
                }
            }
            if (first != null) {
                executeVirtualWithRequestContext(first.action);
            }
        }

        private void scheduleOrEnqueue(ReplayEvent<ReqT> event) {
            boolean executeNow = false;
            synchronized (incomingEvents) {
                if (this.delegate != null && !this.isConsumingFromIncomingEvents && incomingEvents.isEmpty()) {
                    executeNow = true;
                } else {
                    incomingEvents.add(event);
                }
            }
            if (executeNow) {
                executeVirtualWithRequestContext(event.action);
            }
        }

        private void executeVirtualWithRequestContext(Consumer<ServerCall.Listener<ReqT>> consumer) {
            final Context grpcContext = Context.current();
            Callable<Void> blockingHandler = new BlockingExecutionHandler<>(consumer, grpcContext, delegate,
                    requestContextState, getRequestContext(), this);
            if (devMode) {
                blockingHandler = new DevModeBlockingExecutionHandler(Thread.currentThread().getContextClassLoader(),
                        blockingHandler);
            }
            // True while a virtual-thread task is running or is about to run; cleared under
            // incomingEvents lock when the queue is drained (same visibility pattern as the
            // worker path, but this method may also be invoked when chaining from a virtual thread).
            this.isConsumingFromIncomingEvents = true;
            var finalBlockingHandler = blockingHandler;
            virtualThreadExecutor.execute(() -> {
                try {
                    finalBlockingHandler.call();
                    ReplayEvent<ReqT> next;
                    synchronized (incomingEvents) {
                        next = incomingEvents.poll();
                        if (next == null) {
                            this.isConsumingFromIncomingEvents = false;
                        }
                    }
                    if (next != null) {
                        executeVirtualWithRequestContext(next.action);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public void onMessage(ReqT message) {
            scheduleOrEnqueue(new ReplayEvent<>(EventKind.MESSAGE, t -> t.onMessage(message)));
        }

        @Override
        public void onHalfClose() {
            scheduleOrEnqueue(new ReplayEvent<>(EventKind.HALF_CLOSE, ServerCall.Listener::onHalfClose));
        }

        @Override
        public void onCancel() {
            scheduleOrEnqueue(new ReplayEvent<>(EventKind.OTHER, ServerCall.Listener::onCancel));
        }

        @Override
        public void onComplete() {
            scheduleOrEnqueue(new ReplayEvent<>(EventKind.OTHER, ServerCall.Listener::onComplete));
        }

        @Override
        public void onReady() {
            scheduleOrEnqueue(new ReplayEvent<>(EventKind.OTHER, ServerCall.Listener::onReady));
        }
    }

    /**
     * Reorders the head of the queued events so that any {@code onMessage} event(s)
     * arriving before an {@code onHalfClose} are delivered first. The grpc-stub
     * {@code UnaryServerCallHandler} only calls {@code call.request(N)} from inside
     * its {@code startCall}, which this interceptor defers onto a worker / virtual
     * thread. {@code onHalfClose} does not require inbound flow-control budget
     * (END_STREAM is a wire-level marker), so on a cold or backlogged executor the
     * replay listener can observe {@code onHalfClose} before the deframer is allowed
     * to deliver {@code onMessage}. Replaying the queue in arrival order would then
     * cause the underlying unary listener to see a half-close with no request and
     * close the call with {@code Status.INTERNAL: Half-closed without a request}.
     * <p>
     * This method preserves the relative order of all non-message events and the
     * relative order of all message events; it only promotes any {@code MESSAGE}
     * entries that appear after the first {@code HALF_CLOSE} to immediately before
     * it. The caller must hold the queue's monitor.
     */
    private static <ReqT> void reorderForRequestBeforeHalfClose(Deque<ReplayEvent<ReqT>> queue) {
        int halfCloseIndex = -1;
        int i = 0;
        for (ReplayEvent<ReqT> event : queue) {
            if (event.kind == EventKind.HALF_CLOSE) {
                halfCloseIndex = i;
                break;
            }
            i++;
        }
        if (halfCloseIndex < 0) {
            return;
        }
        // Collect MESSAGE events that appear after the first HALF_CLOSE.
        Deque<ReplayEvent<ReqT>> messagesAfterHalfClose = new ArrayDeque<>();
        int idx = 0;
        for (ReplayEvent<ReqT> event : queue) {
            if (idx > halfCloseIndex && event.kind == EventKind.MESSAGE) {
                messagesAfterHalfClose.add(event);
            }
            idx++;
        }
        if (messagesAfterHalfClose.isEmpty()) {
            return; // already ordered correctly - nothing to do
        }
        // Rebuild: prefix before HALF_CLOSE, promoted messages, HALF_CLOSE, remaining non-messages.
        Deque<ReplayEvent<ReqT>> result = new ArrayDeque<>(queue.size());
        idx = 0;
        for (ReplayEvent<ReqT> event : queue) {
            if (idx < halfCloseIndex) {
                result.add(event);
            } else if (idx == halfCloseIndex) {
                result.addAll(messagesAfterHalfClose);
                result.add(event); // the HALF_CLOSE itself
            } else if (event.kind != EventKind.MESSAGE) {
                result.add(event); // non-message events after HALF_CLOSE are preserved
            }
            idx++;
        }
        queue.clear();
        queue.addAll(result);
    }

    private enum EventKind {
        MESSAGE,
        HALF_CLOSE,
        OTHER
    }

    private static final class ReplayEvent<ReqT> {
        final EventKind kind;
        final Consumer<ServerCall.Listener<ReqT>> action;

        ReplayEvent(EventKind kind, Consumer<ServerCall.Listener<ReqT>> action) {
            this.kind = kind;
            this.action = action;
        }
    }

    // protected for tests

    protected boolean isExecutable() {
        return Arc.container() != null;
    }

    protected ManagedContext getRequestContext() {
        return Arc.container().requestContext();
    }
}
