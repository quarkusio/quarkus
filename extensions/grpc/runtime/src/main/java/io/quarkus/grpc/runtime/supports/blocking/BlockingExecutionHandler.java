package io.quarkus.grpc.runtime.supports.blocking;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import io.grpc.Context;
import io.grpc.ServerCall;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.quarkus.grpc.runtime.GrpcContextLocalsProvider;
import io.vertx.core.Vertx;

class BlockingExecutionHandler<ReqT> implements Callable<Void> {
    private final ServerCall.Listener<ReqT> delegate;
    private final Context grpcContext;
    private final Consumer<ServerCall.Listener<ReqT>> consumer;
    private final InjectableContext.ContextState state;
    private final ManagedContext requestContext;
    private final Object lock;

    public BlockingExecutionHandler(Consumer<ServerCall.Listener<ReqT>> consumer, Context grpcContext,
            ServerCall.Listener<ReqT> delegate, InjectableContext.ContextState state,
            ManagedContext requestContext,
            Object lock) {
        this.consumer = consumer;
        this.grpcContext = grpcContext;
        this.delegate = delegate;
        this.state = state;
        this.requestContext = requestContext;
        this.lock = lock;
    }

    @Override
    public Void call() {
        /*
         * We lock here because with client side streaming different messages from the same request
         * might be served by different worker threads. This guarantees memory consistency.
         * The lock object is assumed to be the request's listener
         */
        synchronized (lock) {
            Context previous = grpcContext.attach();
            try {
                requestContext.activate(state);
                try {
                    consumer.accept(delegate);
                } finally {
                    requestContext.deactivate();
                }
                return null;
            } finally {
                grpcContext.detach(previous);
                io.vertx.core.Context vertxDc = Vertx.currentContext();
                if (vertxDc != null) {
                    GrpcContextLocalsProvider.GRPC_CALL_IN_PROGRESS_LOCAL.remove(vertxDc);
                    GrpcContextLocalsProvider.GRPC_CONTEXT_LOCAL.remove(vertxDc);
                }
            }
        }
    }

}
