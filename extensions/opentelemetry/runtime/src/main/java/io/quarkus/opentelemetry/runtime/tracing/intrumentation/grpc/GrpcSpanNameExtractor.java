package io.quarkus.opentelemetry.runtime.tracing.intrumentation.grpc;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;

class GrpcSpanNameExtractor implements SpanNameExtractor<GrpcRequest> {
    @Override
    public String extract(final GrpcRequest grpcRequest) {
        return grpcRequest.getMethodDescriptor().getFullMethodName();
    }
}
