package io.quarkus.opentelemetry.runtime.tracing.grpc;

import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcAttributesGetter;

enum GrpcAttributesGetter implements RpcAttributesGetter<GrpcRequest> {
    INSTANCE;

    @Override
    public String system(final GrpcRequest grpcRequest) {
        return "grpc";
    }

    @Override
    public String service(final GrpcRequest grpcRequest) {
        return grpcRequest.getMethodDescriptor().getServiceName();
    }

    @Override
    public String method(final GrpcRequest grpcRequest) {
        return grpcRequest.getMethodDescriptor().getBareMethodName();
    }
}
