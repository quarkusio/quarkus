package io.quarkus.grpc.auth;

import static io.quarkus.grpc.auth.DefaultAuthExceptionHandlerProvider.transformToStatusException;

import jakarta.enterprise.inject.spi.Prioritized;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.quarkus.grpc.ExceptionHandler;
import io.quarkus.security.AuthenticationException;

/**
 * Exception mapper for authentication and authorization exceptions
 *
 * To alter mapping exceptions, create a subclass of this handler and create an appropriate
 * {@link AuthExceptionHandlerProvider}
 */
public class AuthExceptionHandler<ReqT, RespT> extends ExceptionHandler<ReqT, RespT> implements Prioritized {

    private final boolean addStatusDescription;

    public AuthExceptionHandler(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> serverCall,
            Metadata metadata, boolean addStatusDescription) {
        super(listener, serverCall, metadata);
        this.addStatusDescription = addStatusDescription;
    }

    public AuthExceptionHandler(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> serverCall,
            Metadata metadata) {
        this(listener, serverCall, metadata, false);
    }

    /**
     * Maps exception to a gRPC error. Override this method to customize the mapping
     *
     * @param exception exception thrown
     * @param serverCall server call to close with error
     * @param metadata call metadata
     */
    protected void handleException(Throwable exception, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
        if (exception instanceof AuthenticationException || exception instanceof SecurityException) {
            serverCall.close(transformToStatusException(addStatusDescription, exception), metadata);
        } else if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        } else {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
