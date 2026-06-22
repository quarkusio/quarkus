package io.quarkus.grpc.runtime.stork;

import java.util.concurrent.atomic.AtomicReference;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.smallrye.stork.api.ServiceInstance;

abstract class AbstractStorkMeasuringCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>
        implements StorkMeasuringCollector {
    final boolean recordTime;
    final AtomicReference<ServiceInstance> serviceInstanceRef;

    protected AbstractStorkMeasuringCall(ClientCall<ReqT, RespT> delegate, boolean recordTime,
            AtomicReference<ServiceInstance> serviceInstanceRef) {
        super(delegate);
        this.recordTime = recordTime;
        this.serviceInstanceRef = serviceInstanceRef;
    }

    protected ServiceInstance serviceInstance() {
        return serviceInstanceRef.get();
    }

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
