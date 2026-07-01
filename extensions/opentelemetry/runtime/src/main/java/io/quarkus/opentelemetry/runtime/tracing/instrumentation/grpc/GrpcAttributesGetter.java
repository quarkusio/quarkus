package io.quarkus.opentelemetry.runtime.tracing.instrumentation.grpc;

import io.grpc.Status;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcAttributesGetter;

enum GrpcAttributesGetter implements RpcAttributesGetter<GrpcRequest, Status> {
    INSTANCE;

    @Override
    public String getSystem(final GrpcRequest grpcRequest) {
        return "grpc";
    }

    @Override
    public String getService(final GrpcRequest grpcRequest) {
        return grpcRequest.getMethodDescriptor().getServiceName();
    }

    /**
     * Marked as Deprecated upstream
     *
     * @param grpcRequest
     * @return
     */
    @Deprecated
    @Override
    public String getMethod(final GrpcRequest grpcRequest) {
        return grpcRequest.getMethodDescriptor().getBareMethodName();
    }

    @Override
    public String getRpcMethod(final GrpcRequest grpcRequest) {
        return grpcRequest.getMethodDescriptor().getBareMethodName();
    }
}
