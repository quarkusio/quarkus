package io.quarkus.grpc.runtime.stork;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.Status;

class StorkMeasuringCallListener<RespT> extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {
    final StorkMeasuringCollector collector;

    public StorkMeasuringCallListener(ClientCall.Listener<RespT> responseListener, StorkMeasuringCollector collector) {
        super(responseListener);
        this.collector = collector;
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
        collector.recordEnd(error);
        super.onClose(status, trailers);
    }
}
