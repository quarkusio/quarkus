package io.quarkus.opentelemetry.runtime.tracing.intrumentation.grpc;

import io.grpc.Status;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;

class GrpcSpanStatusExtractor implements SpanStatusExtractor<GrpcRequest, Status> {
    @Override
    public void extract(final SpanStatusBuilder spanStatusBuilder, final GrpcRequest grpcRequest, final Status status,
            final Throwable error) {
        if (status != null && status.isOk()) {
            spanStatusBuilder.setStatus(StatusCode.UNSET);
        } else if (error != null) {
            spanStatusBuilder.setStatus(StatusCode.ERROR);
        } else {
            spanStatusBuilder.setStatus(StatusCode.UNSET);
        }
    }
}
