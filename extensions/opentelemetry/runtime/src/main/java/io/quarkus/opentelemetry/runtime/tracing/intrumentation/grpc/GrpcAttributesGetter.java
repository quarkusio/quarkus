package io.quarkus.opentelemetry.runtime.tracing.intrumentation.grpc;

import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcAttributesGetter;

enum GrpcAttributesGetter implements RpcAttributesGetter<GrpcRequest> {
    INSTANCE;

    @Override
    public String getSystem(final GrpcRequest grpcRequest) {
        return "grpc";
    }

    @Override
    public String getService(final GrpcRequest grpcRequest) {
        return grpcRequest.getMethodDescriptor().getServiceName();
    }

    @Override
    public String getMethod(final GrpcRequest grpcRequest) {
        return grpcRequest.getMethodDescriptor().getBareMethodName();
    }
}
