package io.quarkus.grpc.runtime.supports.blocking;

import java.util.function.Consumer;

import io.grpc.Context;
import io.grpc.ServerCall;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

class BlockingExecutionHandler<ReqT> implements Handler<Promise<Object>> {
    private final ServerCall.Listener<ReqT> delegate;
    private final Context grpcContext;
    private final Consumer<ServerCall.Listener<ReqT>> consumer;
    private final InjectableContext.ContextState state;
    private final ManagedContext requestContext;

    public BlockingExecutionHandler(Consumer<ServerCall.Listener<ReqT>> consumer, Context grpcContext,
            ServerCall.Listener<ReqT> delegate, InjectableContext.ContextState state,
            ManagedContext requestContext) {
        this.consumer = consumer;
        this.grpcContext = grpcContext;
        this.delegate = delegate;
        this.state = state;
        this.requestContext = requestContext;
    }

    @Override
    public void handle(Promise<Object> event) {
        final Context previous = Context.current();
        grpcContext.attach();
        try {
            requestContext.activate(state);
            try {
                consumer.accept(delegate);
            } catch (Throwable any) {
                event.fail(any);
                return;
            } finally {
                requestContext.deactivate();
            }
            event.complete();
        } finally {
            grpcContext.detach(previous);
        }
    }
}
