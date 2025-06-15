package io.quarkus.grpc.runtime.supports.exc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;
import jakarta.inject.Inject;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.ExceptionHandlerProvider;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.grpc.runtime.Interceptors;

@ApplicationScoped
@GlobalInterceptor
public class ExceptionInterceptor implements ServerInterceptor, Prioritized {

    @Inject
    ExceptionHandlerProvider provider;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        return provider.createHandler(next.startCall(call, headers), call, headers);
    }

    @Override
    public int getPriority() {
        return Interceptors.EXCEPTION_HANDLER;
    }
}
