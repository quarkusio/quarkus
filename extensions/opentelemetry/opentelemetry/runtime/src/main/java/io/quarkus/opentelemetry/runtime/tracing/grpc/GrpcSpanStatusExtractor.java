package io.quarkus.opentelemetry.runtime.tracing.grpc;

import io.grpc.Status;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;

class GrpcSpanStatusExtractor implements SpanStatusExtractor<GrpcRequest, Status> {
    @Override
    public StatusCode extract(final GrpcRequest grpcRequest, final Status status, final Throwable error) {
        if (status != null && status.isOk()) {
            return StatusCode.UNSET;
        } else if (error != null) {
            return StatusCode.ERROR;
        }
        return StatusCode.UNSET;
    }
}
