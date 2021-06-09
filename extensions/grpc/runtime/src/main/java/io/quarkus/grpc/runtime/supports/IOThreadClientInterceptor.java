package io.quarkus.grpc.runtime.supports;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Prioritized;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * gRPC Client emissions should be on the event loop if the subscription is executed on the event loop
 */
@ApplicationScoped
public class IOThreadClientInterceptor implements ClientInterceptor, Prioritized {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {

        boolean isOnEventLoop = Context.isOnEventLoopThread();
        Context context = Vertx.currentContext();

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {

                    @Override
                    public void onMessage(RespT message) {
                        if (isOnEventLoop && context != null) {
                            context.runOnContext(unused -> super.onMessage(message));
                        } else {
                            super.onMessage(message);
                        }
                    }

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        if (isOnEventLoop && context != null) {
                            context.runOnContext(unused -> super.onClose(status, trailers));
                        } else {
                            super.onClose(status, trailers);
                        }
                    }
                }, headers);
            }
        };
    }

    @Override
    public int getPriority() {
        // MAX to be sure to be called first.
        return Integer.MAX_VALUE;
    }
}
