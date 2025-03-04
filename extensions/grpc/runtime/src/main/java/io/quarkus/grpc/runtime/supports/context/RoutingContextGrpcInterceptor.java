package io.quarkus.grpc.runtime.supports.context;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ManagedContext;
import io.quarkus.grpc.runtime.Interceptors;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class RoutingContextGrpcInterceptor implements ServerInterceptor, Prioritized {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        Context currentContext = Vertx.currentContext();
        RoutingContext routingContext = currentContext.getLocal(RoutingContext.class.getName());

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(call, headers)) {

            private void invoke(Runnable runnable) {
                ArcContainer container = Arc.container();
                ManagedContext reqContext = container.requestContext();
                CurrentVertxRequest cvr = null;
                if (reqContext.isActive()) {
                    cvr = container.select(CurrentVertxRequest.class).get();
                    cvr.setCurrent(routingContext);
                }
                try {
                    runnable.run();
                } finally {
                    // can be non-active, after onComplete + handleClose -> onHalfClose
                    if (cvr != null && reqContext.isActive()) {
                        cvr.setCurrent(null);
                    }
                }
            }

            @Override
            public void onMessage(ReqT message) {
                invoke(() -> super.onMessage(message));
            }

            @Override
            public void onHalfClose() {
                invoke(super::onHalfClose);
            }

            @Override
            public void onCancel() {
                invoke(super::onCancel);
            }

            @Override
            public void onComplete() {
                invoke(super::onComplete);
            }

            @Override
            public void onReady() {
                invoke(super::onReady);
            }
        };
    }

    @Override
    public int getPriority() {
        return Interceptors.ROUTING_CONTEXT;
    }
}
