package io.quarkus.grpc.auth;

import javax.enterprise.inject.spi.Prioritized;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;

/**
 * Provider for AuthExceptionHandler.
 *
 * To use a custom AuthExceptionHandler, extend {@link AuthExceptionHandler} and implement
 * an {@link AuthExceptionHandlerProvider} with priority greater than the default one.
 */
public interface AuthExceptionHandlerProvider extends Prioritized {
    int DEFAULT_PRIORITY = 0;

    <ReqT, RespT> AuthExceptionHandler<ReqT, RespT> createHandler(Listener<ReqT> listener,
            ServerCall<ReqT, RespT> serverCall, Metadata metadata);
}
