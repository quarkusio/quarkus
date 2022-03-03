package io.quarkus.opentelemetry.runtime.tracing.grpc;

import io.grpc.Status;
import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcAttributesExtractor;

class GrpcAttributesExtractor extends RpcAttributesExtractor<GrpcRequest, Status> {
    @Override
    protected String system(final GrpcRequest grpcRequest) {
        return "grpc";
    }

    @Override
    protected String service(final GrpcRequest grpcRequest) {
        return grpcRequest.getMethodDescriptor().getServiceName();
    }

    @Override
    protected String method(final GrpcRequest grpcRequest) {
        return grpcRequest.getMethodDescriptor().getBareMethodName();
    }
}
