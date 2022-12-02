package io.quarkus.grpc.auth;

import javax.inject.Singleton;

import io.grpc.Metadata;
import io.grpc.ServerCall;

@Singleton
public class DefaultAuthExceptionHandlerProvider implements AuthExceptionHandlerProvider {

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public <ReqT, RespT> AuthExceptionHandler<ReqT, RespT> createHandler(ServerCall.Listener<ReqT> listener,
            ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
        return new AuthExceptionHandler<>(listener, serverCall, metadata);
    }
}
