package io.quarkus.grpc.runtime.supports.blocking;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * gRPC Server interceptor offloading the execution of the gRPC method on a worker thread if the method is annotated
 * with {@link io.smallrye.common.annotation.Blocking}.
 * <p>
 * For non-annotated methods, the interceptor acts as a pass-through.
 */
public class BlockingServerInterceptor implements ServerInterceptor, Function<String, Boolean> {

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
        return blockingMethods.contains(methodName.toLowerCase());
    }

    public Boolean applyVirtual(String name) {
        String methodName = name.substring(name.lastIndexOf("/") + 1);
        return virtualMethods.contains(methodName.toLowerCase());
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
            vertx.executeBlocking(f -> {
                ServerCall.Listener<ReqT> listener;
                try {
                    requestContext.activate(state);
                    listener = next.startCall(call, headers);
                } finally {
                    requestContext.deactivate();
                }
                f.complete(listener);
            }, false,
                    (Handler<AsyncResult<ServerCall.Listener<ReqT>>>) event -> replay.setDelegate(event.result()));

            return replay;
        } else {
            return next.startCall(call, headers);
        }
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

        // exclusive to event loop context
        private ServerCall.Listener<ReqT> delegate;
        private final Queue<Consumer<ServerCall.Listener<ReqT>>> incomingEvents = new LinkedList<>();
        private boolean isConsumingFromIncomingEvents = false;

        private ReplayListener(InjectableContext.ContextState requestContextState) {
            this.requestContextState = requestContextState;
        }

        /**
         * Must be called from within the event loop context
         * If there are deferred events will start executing them in the shared worker context
         *
         * @param delegate the original
         */
        void setDelegate(ServerCall.Listener<ReqT> delegate) {
            this.delegate = delegate;
            if (!this.isConsumingFromIncomingEvents) {
                Consumer<ServerCall.Listener<ReqT>> consumer = incomingEvents.poll();
                if (consumer != null) {
                    executeBlockingWithRequestContext(consumer);
                }
            }
        }

        private void scheduleOrEnqueue(Consumer<ServerCall.Listener<ReqT>> consumer) {
            if (this.delegate != null && !this.isConsumingFromIncomingEvents) {
                executeBlockingWithRequestContext(consumer);
            } else {
                incomingEvents.add(consumer);
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
            Handler<Promise<Object>> blockingHandler = new BlockingExecutionHandler<>(consumer, grpcContext, delegate,
                    requestContextState, getRequestContext(), this);
            if (devMode) {
                blockingHandler = new DevModeBlockingExecutionHandler(Thread.currentThread().getContextClassLoader(),
                        blockingHandler);
            }
            this.isConsumingFromIncomingEvents = true;
            vertx.executeBlocking(blockingHandler, true, p -> {
                Consumer<ServerCall.Listener<ReqT>> next = incomingEvents.poll();
                if (next != null) {
                    executeBlockingWithRequestContext(next);
                } else {
                    this.isConsumingFromIncomingEvents = false;
                }
            });
        }

        @Override
        public void onMessage(ReqT message) {
            scheduleOrEnqueue(t -> t.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            scheduleOrEnqueue(ServerCall.Listener::onHalfClose);
        }

        @Override
        public void onCancel() {
            scheduleOrEnqueue(ServerCall.Listener::onCancel);
        }

        @Override
        public void onComplete() {
            scheduleOrEnqueue(ServerCall.Listener::onComplete);
        }

        @Override
        public void onReady() {
            scheduleOrEnqueue(ServerCall.Listener::onReady);
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
        private final Queue<Consumer<ServerCall.Listener<ReqT>>> incomingEvents = new ConcurrentLinkedQueue<>();
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
            this.delegate = delegate;
            if (!this.isConsumingFromIncomingEvents) {
                Consumer<ServerCall.Listener<ReqT>> consumer = incomingEvents.poll();
                if (consumer != null) {
                    executeVirtualWithRequestContext(consumer);
                }
            }
        }

        private void scheduleOrEnqueue(Consumer<ServerCall.Listener<ReqT>> consumer) {
            if (this.delegate != null && !this.isConsumingFromIncomingEvents) {
                executeVirtualWithRequestContext(consumer);
            } else {
                incomingEvents.add(consumer);
            }
        }

        private void executeVirtualWithRequestContext(Consumer<ServerCall.Listener<ReqT>> consumer) {
            final Context grpcContext = Context.current();
            Handler<Promise<Object>> blockingHandler = new BlockingExecutionHandler<>(consumer, grpcContext, delegate,
                    requestContextState, getRequestContext(), this);
            if (devMode) {
                blockingHandler = new DevModeBlockingExecutionHandler(Thread.currentThread().getContextClassLoader(),
                        blockingHandler);
            }
            this.isConsumingFromIncomingEvents = true;
            Handler<Promise<Object>> finalBlockingHandler = blockingHandler;
            virtualThreadExecutor.execute(() -> {
                finalBlockingHandler.handle(Promise.promise());
                Consumer<ServerCall.Listener<ReqT>> next = incomingEvents.poll();
                if (next != null) {
                    executeVirtualWithRequestContext(next);
                } else {
                    this.isConsumingFromIncomingEvents = false;
                }
            });
        }

        @Override
        public void onMessage(ReqT message) {
            scheduleOrEnqueue(t -> t.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            scheduleOrEnqueue(ServerCall.Listener::onHalfClose);
        }

        @Override
        public void onCancel() {
            scheduleOrEnqueue(ServerCall.Listener::onCancel);
        }

        @Override
        public void onComplete() {
            scheduleOrEnqueue(ServerCall.Listener::onComplete);
        }

        @Override
        public void onReady() {
            scheduleOrEnqueue(ServerCall.Listener::onReady);
        }
    }

    // protected for tests
    protected ManagedContext getRequestContext() {
        return Arc.container().requestContext();
    }
}
