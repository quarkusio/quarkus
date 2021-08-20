package io.quarkus.grpc.server.interceptors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Prioritized;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.quarkus.grpc.GlobalInterceptor;

@ApplicationScoped
@GlobalInterceptor
public class MySecondInterceptor implements ServerInterceptor, Prioritized {

    private volatile long callTime;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
            Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        return serverCallHandler
                .startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(serverCall) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        callTime = System.nanoTime();
                        super.close(status, trailers);
                    }
                }, metadata);
    }

    public long getLastCall() {
        return callTime;
    }

    @Override
    public int getPriority() {
        return 200;
    }
}
