package io.quarkus.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;

/**
 * Generic exception handler
 */
public abstract class ExceptionHandler<ReqT, RespT>
        extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

    private final ServerCall<ReqT, RespT> call;
    private final Metadata metadata;

    public ExceptionHandler(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> call, Metadata metadata) {
        super(listener);
        this.metadata = metadata;
        this.call = call;
    }

    protected abstract void handleException(Throwable t, ServerCall<ReqT, RespT> call, Metadata metadata);

    @Override
    public void onMessage(ReqT message) {
        try {
            super.onMessage(message);
        } catch (Throwable t) {
            handleException(t, call, metadata);
        }
    }

    @Override
    public void onHalfClose() {
        try {
            super.onHalfClose();
        } catch (Throwable t) {
            handleException(t, call, metadata);
        }
    }

    @Override
    public void onReady() {
        try {
            super.onReady();
        } catch (Throwable t) {
            handleException(t, call, metadata);
        }
    }

}
