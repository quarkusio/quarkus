package io.quarkus.grpc.auth;

import jakarta.enterprise.inject.spi.Prioritized;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.StatusException;

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

    /**
     * @param failure security exception this provider can handle according to the {@link #handlesException(Throwable)}
     * @return status exception
     */
    default StatusException transformToStatusException(Throwable failure) {
        // because by default we don't handle any exception
        // the original behavior (before introduction of this method) is kept because 'handlesException' return false
        throw new IllegalStateException("Cannot transform exception " + failure + " to a status exception");
    }

    /**
     * @param failure any gRPC request failure
     * @return whether this provider should create response status for given failure
     */
    default boolean handlesException(Throwable failure) {
        return false;
    }
}
