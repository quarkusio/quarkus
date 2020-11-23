package io.quarkus.grpc.runtime.supports;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Prioritized;

import io.grpc.*;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@ApplicationScoped
public class IOThreadClientInterceptor implements ClientInterceptor, Prioritized {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {

        Context context = Vertx.currentContext();

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onMessage(RespT message) {
                        if (context != null) {
                            context.runOnContext(unused -> super.onMessage(message));
                        } else {
                            super.onMessage(message);
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
