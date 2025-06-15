package io.quarkus.grpc.runtime.stork;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.smallrye.stork.api.ServiceInstance;

abstract class AbstractStorkMeasuringCall<ReqT, RespT>
        extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> implements StorkMeasuringCollector {
    final boolean recordTime;

    protected AbstractStorkMeasuringCall(ClientCall<ReqT, RespT> delegate, boolean recordTime) {
        super(delegate);
        this.recordTime = recordTime;
    }

    protected abstract ServiceInstance serviceInstance();

    public void recordReply() {
        if (serviceInstance() != null && recordTime) {
            serviceInstance().recordReply();
        }
    }

    public void recordEnd(Throwable error) {
        if (serviceInstance() != null) {
            serviceInstance().recordEnd(error);
        }
    }
}
