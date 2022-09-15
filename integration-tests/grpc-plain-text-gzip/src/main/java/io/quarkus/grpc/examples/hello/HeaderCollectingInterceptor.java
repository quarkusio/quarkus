package io.quarkus.grpc.examples.hello;

import jakarta.enterprise.context.ApplicationScoped;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.internal.GrpcUtil;
import io.quarkus.grpc.GlobalInterceptor;

@GlobalInterceptor
@ApplicationScoped
public class HeaderCollectingInterceptor implements ClientInterceptor {
    private volatile String encoding;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(
                        new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                            @Override
                            public void onHeaders(Metadata headers) {
                                encoding = headers.get(GrpcUtil.MESSAGE_ENCODING_KEY);
                                super.onHeaders(headers);
                            }
                        }, headers);
            }
        };
    }

    public String getEncoding() {
        return encoding;
    }

    public void clear() {
        encoding = null;
    }

}
