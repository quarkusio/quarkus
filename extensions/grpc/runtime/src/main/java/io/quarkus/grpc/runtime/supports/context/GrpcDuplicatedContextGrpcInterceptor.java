package io.quarkus.grpc.runtime.supports.context;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;

import org.jboss.logging.Logger;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.quarkus.grpc.GlobalInterceptor;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@ApplicationScoped
@GlobalInterceptor
public class GrpcDuplicatedContextGrpcInterceptor implements ServerInterceptor, Prioritized {
    private static final Logger log = Logger.getLogger(GrpcDuplicatedContextGrpcInterceptor.class.getName());

    public GrpcDuplicatedContextGrpcInterceptor() {
    }

    private static boolean isRootContext(Context context) {
        return !VertxContext.isDuplicatedContext(context);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // This interceptor is called first, so, we should be on the event loop.
        Context capturedVertxContext = Vertx.currentContext();

        if (capturedVertxContext != null) {
            // If we are not on a duplicated context, create and switch.
            Context local = VertxContext.getOrCreateDuplicatedContext(capturedVertxContext);
            setContextSafe(local, true);

            // Must be sure to call next.startCall on the right context
            return new ListenedOnDuplicatedContext<>(call, () -> next.startCall(call, headers), local);
        } else {
            log.warn("Unable to run on a duplicated context - interceptor not called on the Vert.x event loop");
            return next.startCall(call, headers);
        }
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    static class ListenedOnDuplicatedContext<ReqT, RespT> extends ServerCall.Listener<ReqT> {

        private final Context context;
        private final Supplier<ServerCall.Listener<ReqT>> supplier;
        private final ServerCall<ReqT, RespT> call;
        private ServerCall.Listener<ReqT> delegate;

        private final AtomicBoolean closed = new AtomicBoolean();

        public ListenedOnDuplicatedContext(ServerCall<ReqT, RespT> call, Supplier<ServerCall.Listener<ReqT>> supplier,
                Context context) {
            this.context = context;
            this.supplier = supplier;
            this.call = call;
        }

        private synchronized ServerCall.Listener<ReqT> getDelegate() {
            if (delegate == null) {
                try {
                    delegate = supplier.get();
                } catch (Throwable t) {
                    // If the interceptor supplier throws an exception, catch it, and close the call.
                    log.warn("Unable to retrieve gRPC Server call listener", t);
                    close(t);
                    return null;
                }
            }
            return delegate;
        }

        private void close(Throwable t) {
            if (closed.compareAndSet(false, true)) {
                call.close(Status.fromThrowable(t), new Metadata());
            }
        }

        private void invoke(Consumer<ServerCall.Listener<ReqT>> invocation) {
            if (Vertx.currentContext() == context) {
                ServerCall.Listener<ReqT> listener = getDelegate();
                if (listener == null) {
                    return;
                }
                try {
                    invocation.accept(listener);
                } catch (Throwable t) {
                    close(t);
                }
            } else {
                context.runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void x) {
                        ServerCall.Listener<ReqT> listener = ListenedOnDuplicatedContext.this.getDelegate();
                        if (listener == null) {
                            return;
                        }
                        try {
                            invocation.accept(listener);
                        } catch (Throwable t) {
                            close(t);
                        }
                    }
                });
            }
        }

        @Override
        public void onMessage(ReqT message) {
            invoke(new Consumer<ServerCall.Listener<ReqT>>() {
                @Override
                public void accept(ServerCall.Listener<ReqT> listener) {
                    listener.onMessage(message);
                }
            });
        }

        @Override
        public void onReady() {
            invoke(ServerCall.Listener::onReady);
        }

        @Override
        public void onHalfClose() {
            invoke(ServerCall.Listener::onHalfClose);
        }

        @Override
        public void onCancel() {
            invoke(ServerCall.Listener::onCancel);
        }

        @Override
        public void onComplete() {
            invoke(ServerCall.Listener::onComplete);
        }
    }
}
