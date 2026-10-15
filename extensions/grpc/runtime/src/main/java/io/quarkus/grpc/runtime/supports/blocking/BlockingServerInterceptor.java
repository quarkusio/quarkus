package io.quarkus.grpc.runtime.supports.blocking;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
import io.grpc.MethodDescriptor;
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
            boolean deferHalfCloseUntilMessage = deferHalfCloseUntilMessage(call.getMethodDescriptor().getType());
            VirtualReplayListener<ReqT> replay = new VirtualReplayListener<>(state, deferHalfCloseUntilMessage);
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
            boolean deferHalfCloseUntilMessage = deferHalfCloseUntilMessage(call.getMethodDescriptor().getType());
            ReplayListener<ReqT> replay = new ReplayListener<>(state, deferHalfCloseUntilMessage);
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
        private final boolean deferHalfCloseUntilMessage;

        // exclusive to event loop context
        private volatile ServerCall.Listener<ReqT> delegate;
        // Guarded by its own monitor. Events queued before setDelegate runs may arrive
        // out of order versus how the underlying UnaryServerCallListener expects them
        // (e.g. onHalfClose before onMessage when call.request(N) was deferred — see
        // setDelegate). The deque is reordered once at delegate-injection time to
        // preserve the request-before-half-close invariant before draining.
        private final Deque<ReplayEvent<ReqT>> incomingEvents = new ArrayDeque<>();
        private volatile boolean isConsumingFromIncomingEvents;
        private boolean messageReceived;

        private ReplayListener(InjectableContext.ContextState requestContextState, boolean deferHalfCloseUntilMessage) {
            this.requestContextState = requestContextState;
            this.grpcContext = Context.current();
            this.deferHalfCloseUntilMessage = deferHalfCloseUntilMessage;
        }

        /**
         * Must be called from within the event loop context
         * If there are deferred events will start executing them in the shared worker context
         *
         * @param delegate the original
         */
        void setDelegate(ServerCall.Listener<ReqT> delegate) {
            synchronized (incomingEvents) {
                this.delegate = delegate;
            }
            tryStartDraining();
        }

        private void tryStartDraining() {
            ReplayEvent<ReqT> first = null;
            synchronized (incomingEvents) {
                if (this.delegate == null || this.isConsumingFromIncomingEvents) {
                    return;
                }
                if (!prepareDrainQueue(deferHalfCloseUntilMessage, messageReceived, incomingEvents)) {
                    return;
                }
                first = incomingEvents.poll();
                if (first != null) {
                    this.isConsumingFromIncomingEvents = true;
                }
            }
            if (first != null) {
                executeBlockingWithRequestContext(first.action);
            }
        }

        private void scheduleOrEnqueue(ReplayEvent<ReqT> event) {
            Consumer<ServerCall.Listener<ReqT>> toRunDirectly = null;
            synchronized (incomingEvents) {
                if (event.kind == EventKind.MESSAGE) {
                    messageReceived = true;
                }
                boolean canRunDirectly = this.delegate != null
                        && !this.isConsumingFromIncomingEvents
                        && incomingEvents.isEmpty()
                        && !(deferHalfCloseUntilMessage && event.kind == EventKind.HALF_CLOSE && !messageReceived);
                if (canRunDirectly) {
                    toRunDirectly = event.action;
                    this.isConsumingFromIncomingEvents = true;
                } else {
                    incomingEvents.add(event);
                }
            }
            if (toRunDirectly != null) {
                executeBlockingWithRequestContext(toRunDirectly);
            } else {
                tryStartDraining();
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
            // Usually already true: setDelegate / scheduleOrEnqueue set it under
            // incomingEvents before dispatching. This write keeps the worker-chain path
            // consistent when chaining from executeBlocking's onComplete handler.
            this.isConsumingFromIncomingEvents = true;
            vertx.executeBlocking(blockingHandler, false).onComplete(p -> {
                ReplayEvent<ReqT> next;
                synchronized (incomingEvents) {
                    ReplayEvent<ReqT> polled = null;
                    if (prepareDrainQueue(deferHalfCloseUntilMessage, messageReceived, incomingEvents)) {
                        polled = incomingEvents.poll();
                    }
                    next = polled;
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
     * are executed sequentially.
     * <p>
     * This replay listener is only used for virtual threads.
     */
    private class VirtualReplayListener<ReqT> extends ServerCall.Listener<ReqT> {
        private final InjectableContext.ContextState requestContextState;
        private final boolean deferHalfCloseUntilMessage;

        // exclusive to event loop context
        private ServerCall.Listener<ReqT> delegate;
        // Guarded by its own monitor. See ReplayListener#incomingEvents for the
        // rationale on why this is reordered before draining.
        private final Deque<ReplayEvent<ReqT>> incomingEvents = new ArrayDeque<>();
        private volatile boolean isConsumingFromIncomingEvents = false;
        private boolean messageReceived;

        private VirtualReplayListener(InjectableContext.ContextState requestContextState,
                boolean deferHalfCloseUntilMessage) {
            this.requestContextState = requestContextState;
            this.deferHalfCloseUntilMessage = deferHalfCloseUntilMessage;
        }

        /**
         * Must be called from within the event loop context
         * If there are deferred events will start executing them in the shared worker context
         *
         * @param delegate the original
         */
        void setDelegate(ServerCall.Listener<ReqT> delegate) {
            synchronized (incomingEvents) {
                this.delegate = delegate;
            }
            tryStartDraining();
        }

        private void tryStartDraining() {
            ReplayEvent<ReqT> first = null;
            synchronized (incomingEvents) {
                if (this.delegate == null || this.isConsumingFromIncomingEvents) {
                    return;
                }
                if (!prepareDrainQueue(deferHalfCloseUntilMessage, messageReceived, incomingEvents)) {
                    return;
                }
                first = incomingEvents.poll();
                if (first != null) {
                    this.isConsumingFromIncomingEvents = true;
                }
            }
            if (first != null) {
                executeVirtualWithRequestContext(first.action);
            }
        }

        private void scheduleOrEnqueue(ReplayEvent<ReqT> event) {
            Consumer<ServerCall.Listener<ReqT>> toRunDirectly = null;
            synchronized (incomingEvents) {
                if (event.kind == EventKind.MESSAGE) {
                    messageReceived = true;
                }
                boolean canRunDirectly = this.delegate != null
                        && !this.isConsumingFromIncomingEvents
                        && incomingEvents.isEmpty()
                        && !(deferHalfCloseUntilMessage && event.kind == EventKind.HALF_CLOSE && !messageReceived);
                if (canRunDirectly) {
                    toRunDirectly = event.action;
                    this.isConsumingFromIncomingEvents = true;
                } else {
                    incomingEvents.add(event);
                }
            }
            if (toRunDirectly != null) {
                executeVirtualWithRequestContext(toRunDirectly);
            } else {
                tryStartDraining();
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
            // Usually already true: setDelegate / scheduleOrEnqueue set it under incomingEvents
            // before dispatching. Kept for the chain path that calls here after polling the next
            // event. Cleared under incomingEvents when the queue is drained.
            this.isConsumingFromIncomingEvents = true;
            var finalBlockingHandler = blockingHandler;
            virtualThreadExecutor.execute(() -> {
                try {
                    finalBlockingHandler.call();
                    ReplayEvent<ReqT> next;
                    synchronized (incomingEvents) {
                        ReplayEvent<ReqT> polled = null;
                        if (prepareDrainQueue(deferHalfCloseUntilMessage, messageReceived, incomingEvents)) {
                            polled = incomingEvents.poll();
                        }
                        next = polled;
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
     * Unary and server-streaming RPCs require an inbound request message before the client's
     * half-close can be delivered to the grpc-stub listener. Client-streaming and bidi may
     * legitimately half-close with zero messages (empty client stream).
     */
    private static boolean deferHalfCloseUntilMessage(MethodDescriptor.MethodType type) {
        return type == MethodDescriptor.MethodType.UNARY
                || type == MethodDescriptor.MethodType.SERVER_STREAMING;
    }

    /**
     * Prepares the event queue for draining. Always reorders messages ahead of half-close for
     * every method type; optionally blocks draining when a unary / server-streaming call has
     * not yet received its request message.
     *
     * @return {@code true} if the caller may poll the queue; {@code false} if draining must wait
     */
    private static <ReqT> boolean prepareDrainQueue(boolean deferHalfCloseUntilMessage, boolean messageReceived,
            Deque<ReplayEvent<ReqT>> queue) {
        reorderForRequestBeforeHalfClose(queue);
        return !deferHalfCloseUntilMessage || messageReceived;
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
        int size = queue.size();
        if (size == 0) {
            return;
        }
        // Snapshot once so we do not walk the deque multiple times.
        List<ReplayEvent<ReqT>> snapshot = new ArrayList<>(queue);
        int halfCloseIdx = -1;
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i).kind == EventKind.HALF_CLOSE) {
                halfCloseIdx = i;
                break;
            }
        }
        if (halfCloseIdx < 0) {
            return;
        }
        boolean hasTrailingMessage = false;
        for (int i = halfCloseIdx + 1; i < snapshot.size(); i++) {
            if (snapshot.get(i).kind == EventKind.MESSAGE) {
                hasTrailingMessage = true;
                break;
            }
        }
        if (!hasTrailingMessage) {
            return;
        }
        Deque<ReplayEvent<ReqT>> result = new ArrayDeque<>(snapshot.size());
        for (int i = 0; i < halfCloseIdx; i++) {
            result.addLast(snapshot.get(i));
        }
        for (int i = halfCloseIdx + 1; i < snapshot.size(); i++) {
            ReplayEvent<ReqT> e = snapshot.get(i);
            if (e.kind == EventKind.MESSAGE) {
                result.addLast(e);
            }
        }
        result.addLast(snapshot.get(halfCloseIdx));
        for (int i = halfCloseIdx + 1; i < snapshot.size(); i++) {
            ReplayEvent<ReqT> e = snapshot.get(i);
            if (e.kind != EventKind.MESSAGE) {
                result.addLast(e);
            }
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
