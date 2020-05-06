package io.quarkus.grpc.client.interceptors;

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

@ApplicationScoped
public class MySecondClientInterceptor implements ClientInterceptor, Prioritized {

    private volatile long callTime;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(
                        new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {

                            @Override
                            protected Listener<RespT> delegate() {
                                callTime = System.nanoTime();
                                return super.delegate();
                            }
                        }, headers);
            }
        };
    }

    public long getLastCall() {
        return callTime;
    }

    @Override
    public int getPriority() {
        return 200;
    }
}
