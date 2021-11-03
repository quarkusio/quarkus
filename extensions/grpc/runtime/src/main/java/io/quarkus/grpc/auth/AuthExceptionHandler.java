package io.quarkus.grpc.auth;

import javax.enterprise.inject.spi.Prioritized;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.quarkus.security.AuthenticationFailedException;

/**
 * Exception mapper for authentication and authorization exceptions
 *
 * To alter mapping exceptions, create a subclass of this handler and create an appropriate
 * {@link AuthExceptionHandlerProvider}
 */
public class AuthExceptionHandler<ReqT, RespT>
        extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> implements Prioritized {

    private final ServerCall<ReqT, RespT> serverCall;
    private final Metadata metadata;

    public AuthExceptionHandler(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> serverCall,
            Metadata metadata) {
        super(listener);
        this.metadata = metadata;
        this.serverCall = serverCall;
    }

    @Override
    public void onMessage(ReqT message) {
        try {
            super.onMessage(message);
        } catch (RuntimeException e) {
            handleException(e, serverCall, metadata);
        }
    }

    @Override
    public void onHalfClose() {
        try {
            super.onHalfClose();
        } catch (RuntimeException e) {
            handleException(e, serverCall, metadata);
        }
    }

    @Override
    public void onReady() {
        try {
            super.onReady();
        } catch (RuntimeException e) {
            handleException(e, serverCall, metadata);
        }
    }

    /**
     * Maps exception to a gRPC error. Override this method to customize the mapping
     *
     * @param exception exception thrown
     * @param serverCall server call to close with error
     * @param metadata call metadata
     */
    protected void handleException(RuntimeException exception, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
        if (exception instanceof AuthenticationFailedException) {
            serverCall.close(Status.UNAUTHENTICATED.withDescription(exception.getMessage()), metadata);
        } else if (exception instanceof SecurityException) {
            serverCall.close(Status.PERMISSION_DENIED.withDescription(exception.getMessage()), metadata);
        } else {
            throw exception;
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
