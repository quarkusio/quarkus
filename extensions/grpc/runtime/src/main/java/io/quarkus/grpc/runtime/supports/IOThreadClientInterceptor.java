package io.quarkus.grpc.runtime.supports;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.quarkus.grpc.GlobalInterceptor;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * gRPC Client emissions should be on the event loop if the subscription is executed on the event loop
 */
@GlobalInterceptor
@ApplicationScoped
public class IOThreadClientInterceptor implements ClientInterceptor, Prioritized {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {

                Context context = Vertx.currentContext();
                boolean isOnIOThread = context != null && Context.isOnEventLoopThread();

                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {

                    @Override
                    public void onReady() {
                        if (isOnIOThread) {
                            context.runOnContext(unused -> super.onReady());
                        } else {
                            super.onReady();
                        }
                    }

                    @Override
                    public void onHeaders(Metadata headers) {
                        if (isOnIOThread) {
                            context.runOnContext(unused -> super.onHeaders(headers));
                        } else {
                            super.onHeaders(headers);
                        }
                    }

                    @Override
                    public void onMessage(RespT message) {
                        if (isOnIOThread) {
                            context.runOnContext(unused -> super.onMessage(message));
                        } else {
                            super.onMessage(message);
                        }
                    }

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        if (isOnIOThread) {
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
