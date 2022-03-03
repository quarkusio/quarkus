package io.quarkus.opentelemetry.runtime.tracing.grpc;

import io.grpc.Status;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

class GrpcStatusCodeExtractor implements AttributesExtractor<GrpcRequest, Status> {
    @Override
    public void onStart(
            final AttributesBuilder attributes,
            final Context parentContext,
            final GrpcRequest grpcRequest) {

    }

    @Override
    public void onEnd(
            final AttributesBuilder attributes,
            final Context context,
            final GrpcRequest grpcRequest,
            final Status status,
            final Throwable error) {

        if (status != null) {
            set(attributes, SemanticAttributes.RPC_GRPC_STATUS_CODE, (long) (status.getCode().value()));
        }
    }
}
