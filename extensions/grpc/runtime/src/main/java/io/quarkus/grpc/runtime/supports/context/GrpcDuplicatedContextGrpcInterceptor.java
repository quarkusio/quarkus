package io.quarkus.grpc.runtime.supports.context;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;

import org.jboss.logging.Logger;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
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
            return new ListenedOnDuplicatedContext<>(() -> next.startCall(call, headers), local);
        } else {
            log.warn("Unable to run on a duplicated context - interceptor not called on the Vert.x event loop");
            return next.startCall(call, headers);
        }
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    static class ListenedOnDuplicatedContext<ReqT> extends ServerCall.Listener<ReqT> {

        private final Context context;
        private final Supplier<ServerCall.Listener<ReqT>> supplier;
        private ServerCall.Listener<ReqT> delegate;

        public ListenedOnDuplicatedContext(Supplier<ServerCall.Listener<ReqT>> supplier, Context context) {
            this.context = context;
            this.supplier = supplier;
        }

        private synchronized ServerCall.Listener<ReqT> getDelegate() {
            if (delegate == null) {
                delegate = supplier.get();
            }
            return delegate;
        }

        @Override
        public void onMessage(ReqT message) {
            if (Vertx.currentContext() == context) {
                getDelegate().onMessage(message);
            } else {
                context.runOnContext(x -> getDelegate().onMessage(message));
            }
        }

        @Override
        public void onReady() {
            if (Vertx.currentContext() == context) {
                getDelegate().onReady();
            } else {
                context.runOnContext(x -> getDelegate().onReady());
            }
        }

        @Override
        public void onHalfClose() {
            if (Vertx.currentContext() == context) {
                getDelegate().onHalfClose();
            } else {
                context.runOnContext(x -> getDelegate().onHalfClose());
            }
        }

        @Override
        public void onCancel() {
            if (Vertx.currentContext() == context) {
                getDelegate().onCancel();
            } else {
                context.runOnContext(x -> getDelegate().onCancel());
            }
        }

        @Override
        public void onComplete() {
            if (Vertx.currentContext() == context) {
                getDelegate().onComplete();
            } else {
                context.runOnContext(x -> getDelegate().onComplete());
            }
        }
    }
}
