package io.quarkus.opentelemetry.runtime.tracing.intrumentation.grpc;

import io.grpc.Status;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.SemanticAttributes;

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
            attributes.put(SemanticAttributes.RPC_GRPC_STATUS_CODE, status.getCode().value());
        }
    }
}
