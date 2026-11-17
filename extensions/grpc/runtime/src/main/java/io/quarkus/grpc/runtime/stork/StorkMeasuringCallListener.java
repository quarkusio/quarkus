package io.quarkus.grpc.runtime.stork;

import org.jboss.logging.Logger;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.Status;

class StorkMeasuringCallListener<RespT>
        extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {
    private static final Logger log = Logger.getLogger(StorkMeasuringCallListener.class);

    final StorkMeasuringCollector collector;

    public StorkMeasuringCallListener(ClientCall.Listener<RespT> responseListener, StorkMeasuringCollector collector) {
        super(responseListener);
        this.collector = collector;
    }

    @Override
    public void onMessage(RespT message) {
        try {
            collector.recordReply();
        } catch (RuntimeException e) {
            // Observability failures must never propagate to the gRPC framework
            log.debug("Failed to record Stork reply observation", e);
        } finally {
            super.onMessage(message);
        }
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
        Exception error = null;
        if (!status.isOk()) {
            error = status.asException(trailers);
        }
        try {
            collector.recordEnd(error);
        } catch (RuntimeException e) {
            // Observability failures must never propagate to the gRPC framework
            log.debug("Failed to record Stork end observation", e);
        } finally {
            super.onClose(status, trailers);
        }
    }
}
