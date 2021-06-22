package io.quarkus.grpc.runtime.supports.blocking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * gRPC Server interceptor offloading the execution of the gRPC method on a worker thread if the method is annotated
 * with {@link io.smallrye.common.annotation.Blocking}.
 *
 * For non-annotated methods, the interceptor acts as a pass-through.
 */
public class BlockingServerInterceptor implements ServerInterceptor, Function<String, Boolean> {

    private final Vertx vertx;
    private final List<String> blockingMethods;
    private final Map<String, Boolean> cache = new HashMap<>();
    private final boolean devMode;

    public BlockingServerInterceptor(Vertx vertx, List<String> blockingMethods, boolean devMode) {
        this.vertx = vertx;
        this.blockingMethods = new ArrayList<>();
        this.devMode = devMode;
        for (String method : blockingMethods) {
            this.blockingMethods.add(method.toLowerCase());
        }
    }

    @Override
    public Boolean apply(String name) {
        String methodName = name.substring(name.lastIndexOf("/") + 1);
        return blockingMethods.contains(methodName.toLowerCase());
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // We need to check if the method is annotated with @Blocking.
        // Unfortunately, we can't have the Java method object, we can only have the gRPC full method name.
        // We extract the method name, and check if the name if is the list.
        // This makes the following assumptions:
        // 1. the code generator does not change the method name (which makes sense)
        // 2. the method name is unique, which is a constraint of gRPC

        // For performance purpose, we execute the lookup only once
        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        boolean isBlocking = cache.computeIfAbsent(fullMethodName, this);

        if (isBlocking) {
            final ManagedContext requestContext = getRequestContext();
            // context should always be active here
            // it is initialized by io.quarkus.grpc.runtime.supports.context.GrpcRequestContextGrpcInterceptor
            // that should always be called before this interceptor
            ContextState state = requestContext.getState();
            ReplayListener<ReqT> replay = new ReplayListener<>(state);
            vertx.executeBlocking(new Handler<Promise<Object>>() {
                @Override
                public void handle(Promise<Object> f) {
                    ServerCall.Listener<ReqT> listener;
                    try {
                        requestContext.activate(state);
                        listener = next.startCall(call, headers);
                    } finally {
                        requestContext.deactivate();
                    }
                    replay.setDelegate(listener, requestContext);
                    f.complete(null);
                }
            }, null);

            return replay;
        } else {
            return next.startCall(call, headers);
        }
    }

    /**
     * Stores the incoming events until the listener is injected.
     * When injected, replay the events.
     *
     * Note that event must be executed in order, explaining the `ordered:true`.
     */
    private class ReplayListener<ReqT> extends ServerCall.Listener<ReqT> {
        private ServerCall.Listener<ReqT> delegate;
        private final List<Consumer<ServerCall.Listener<ReqT>>> incomingEvents = new ArrayList<>();
        private final ContextState requestContextState;

        private ReplayListener(ContextState requestContextState) {
            this.requestContextState = requestContextState;
        }

        synchronized void setDelegate(ServerCall.Listener<ReqT> delegate,
                ManagedContext requestContext) {
            this.delegate = delegate;
            requestContext.activate(requestContextState);
            try {
                for (Consumer<ServerCall.Listener<ReqT>> event : incomingEvents) {
                    event.accept(delegate);
                }
            } finally {
                requestContext.deactivate();
            }
            incomingEvents.clear();
        }

        private synchronized void executeOnContextOrEnqueue(Consumer<ServerCall.Listener<ReqT>> consumer) {
            if (this.delegate != null) {
                executeBlockingWithRequestContext(consumer);
            } else {
                incomingEvents.add(consumer);
            }
        }

        private void executeBlockingWithRequestContext(Consumer<ServerCall.Listener<ReqT>> consumer) {
            final Context grpcContext = Context.current();
            Handler<Promise<Object>> blockingHandler = new BlockingExecutionHandler<>(consumer, grpcContext, delegate,
                    requestContextState, getRequestContext());
            if (devMode) {
                blockingHandler = new DevModeBlockingExecutionHandler(Thread.currentThread().getContextClassLoader(),
                        blockingHandler);
            }
            vertx.executeBlocking(blockingHandler, true, null);
        }

        @Override
        public void onMessage(ReqT message) {
            executeOnContextOrEnqueue(new Consumer<ServerCall.Listener<ReqT>>() {
                @Override
                public void accept(ServerCall.Listener<ReqT> t) {
                    t.onMessage(message);
                }
            });
        }

        @Override
        public void onHalfClose() {
            executeOnContextOrEnqueue(ServerCall.Listener::onHalfClose);
        }

        @Override
        public void onCancel() {
            executeOnContextOrEnqueue(ServerCall.Listener::onCancel);
        }

        @Override
        public void onComplete() {
            executeOnContextOrEnqueue(ServerCall.Listener::onComplete);
        }

        @Override
        public void onReady() {
            executeOnContextOrEnqueue(ServerCall.Listener::onReady);
        }
    }

    // protected for tests
    protected ManagedContext getRequestContext() {
        return Arc.container().requestContext();
    }
}
