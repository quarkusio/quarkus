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

    @Override
    public void recordReply() {
        ServiceInstance si = serviceInstance();
        if (si != null && recordTime) {
            si.recordReply();
        }
    }

    @Override
    public void recordEnd(Throwable error) {
        ServiceInstance si = serviceInstance();
        if (si != null) {
            si.recordEnd(error);
        }
    }
}
