package io.quarkus.opentelemetry.runtime.tracing.grpc;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class GrpcRequest {
    private final MethodDescriptor<?, ?> methodDescriptor;
    private final Metadata metadata;
    private final Attributes attributes;

    private GrpcRequest(
            final MethodDescriptor<?, ?> methodDescriptor,
            final Metadata metadata,
            final Attributes attributes) {
        this.methodDescriptor = methodDescriptor;
        this.metadata = metadata;
        this.attributes = attributes;
    }

    public MethodDescriptor<?, ?> getMethodDescriptor() {
        return methodDescriptor;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public static GrpcRequest server(
            final MethodDescriptor<?, ?> methodDescriptor,
            final Metadata metadata,
            final Attributes attributes) {
        return new GrpcRequest(methodDescriptor, metadata, attributes);
    }

    public static GrpcRequest client(final MethodDescriptor<?, ?> methodDescriptor) {
        return new GrpcRequest(methodDescriptor, null, null);
    }

    public static GrpcRequest client(final MethodDescriptor<?, ?> methodDescriptor, final Metadata metadata) {
        return new GrpcRequest(methodDescriptor, metadata, null);
    }
}
