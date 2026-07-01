package io.quarkus.grpc.runtime.stork;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.Status;

class StorkMeasuringCallListener<RespT>
        extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {
    final StorkMeasuringCollector collector;
    final Runnable onClose;

    public StorkMeasuringCallListener(ClientCall.Listener<RespT> responseListener, StorkMeasuringCollector collector) {
        this(responseListener, collector, null);
    }

    public StorkMeasuringCallListener(ClientCall.Listener<RespT> responseListener, StorkMeasuringCollector collector,
            Runnable onClose) {
        super(responseListener);
        this.collector = collector;
        this.onClose = onClose;
    }

    @Override
    public void onMessage(RespT message) {
        collector.recordReply();
        super.onMessage(message);
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
        Exception error = null;
        if (!status.isOk()) {
            error = status.asException(trailers);
        }
        try {
            collector.recordEnd(error);
        } finally {
            if (onClose != null) {
                onClose.run();
            }
        }
        super.onClose(status, trailers);
    }
}
