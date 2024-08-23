package io.quarkus.grpc.examples.interceptors;

import jakarta.enterprise.context.ApplicationScoped;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.quarkus.grpc.GlobalInterceptor;

@ApplicationScoped
@GlobalInterceptor
public class PriorityImplInterceptor implements PriorityInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        HelloWorldEndpoint.invoked.add(getClass().getName());
        return next.startCall(call, headers);
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
