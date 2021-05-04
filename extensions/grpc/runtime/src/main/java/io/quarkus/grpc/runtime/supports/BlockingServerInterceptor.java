package io.quarkus.grpc.runtime.supports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * gRPC Server interceptor offloading the execution of the gRPC method on a wroker thread if the method is annotated
 * with {@link io.smallrye.common.annotation.Blocking}.
 *
 * For non-annotated methods, the interceptor acts as a pass-through.
 */
public class BlockingServerInterceptor implements ServerInterceptor {

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
        boolean isBlocking = cache.computeIfAbsent(fullMethodName, new Function<String, Boolean>() {
            @Override
            public Boolean apply(String name) {
                String methodName = name.substring(name.lastIndexOf("/") + 1);
                return blockingMethods.contains(methodName.toLowerCase());
            }
        });

        if (isBlocking) {
            ReplayListener<ReqT> replay = new ReplayListener<>();

            vertx.executeBlocking(new Handler<Promise<Object>>() {
                @Override
                public void handle(Promise<Object> f) {
                    ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
                    replay.setDelegate(listener);
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
        private final List<Consumer<ServerCall.Listener<ReqT>>> incomingEvents = new LinkedList<>();

        synchronized void setDelegate(ServerCall.Listener<ReqT> delegate) {
            this.delegate = delegate;
            for (Consumer<ServerCall.Listener<ReqT>> event : incomingEvents) {
                event.accept(delegate);
            }
            incomingEvents.clear();
        }

        private synchronized void executeOnContextOrEnqueue(Consumer<ServerCall.Listener<ReqT>> consumer) {
            if (this.delegate != null) {
                final Context grpcContext = Context.current();
                Handler<Promise<Object>> blockingHandler = new BlockingExecutionHandler<>(consumer, grpcContext, delegate);
                if (devMode) {
                    blockingHandler = new DevModeBlockingExecutionHandler<ReqT>(Thread.currentThread().getContextClassLoader(),
                            blockingHandler);
                }
                vertx.executeBlocking(blockingHandler, true, null);
            } else {
                incomingEvents.add(consumer);
            }
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

    private static class DevModeBlockingExecutionHandler<ReqT> implements Handler<Promise<Object>> {

        final ClassLoader tccl;
        final Handler<Promise<Object>> delegate;

        public DevModeBlockingExecutionHandler(ClassLoader tccl, Handler<Promise<Object>> delegate) {
            this.tccl = tccl;
            this.delegate = delegate;
        }

        @Override
        public void handle(Promise<Object> event) {
            ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(tccl);
            try {
                delegate.handle(event);
            } finally {
                Thread.currentThread().setContextClassLoader(originalTccl);
            }
        }
    }

    private static class BlockingExecutionHandler<ReqT> implements Handler<Promise<Object>> {
        private final ServerCall.Listener<ReqT> delegate;
        private final Context grpcContext;
        private final Consumer<ServerCall.Listener<ReqT>> consumer;

        public BlockingExecutionHandler(Consumer<ServerCall.Listener<ReqT>> consumer, Context grpcContext,
                ServerCall.Listener<ReqT> delegate) {
            this.consumer = consumer;
            this.grpcContext = grpcContext;
            this.delegate = delegate;
        }

        @Override
        public void handle(Promise<Object> event) {
            final Context previous = Context.current();
            grpcContext.attach();
            try {
                consumer.accept(delegate);
                event.complete();
            } finally {
                grpcContext.detach(previous);
            }
        }
    }
}
