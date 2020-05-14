package io.quarkus.grpc.runtime.annotations;

import javax.enterprise.util.AnnotationLiteral;

/**
 * Supports inline instantiation of the {@link GrpcService} qualifier.
 */
public final class GrpcServiceLiteral extends AnnotationLiteral<GrpcService> implements GrpcService {

    private static final long serialVersionUID = 1L;

    private final String value;

    /**
     * Creates a new instance of {@link GrpcServiceLiteral}.
     *
     * @param value the name of the {@code gRPC Service}, must not be {@code null}, must not be {@code blank}
     * @return the {@link GrpcServiceLiteral} instance.
     */
    public static GrpcServiceLiteral of(String value) {
        return new GrpcServiceLiteral(value);
    }

    /**
     * Creates a new instance of {@link GrpcServiceLiteral}.
     * Users should use the {@link #of(String)} method to create instances.
     *
     * @param value the value.
     */
    GrpcServiceLiteral(String value) {
        this.value = value;
    }

    /**
     * @return the {@code gRPC Service} name.
     */
    public String value() {
        return value;
    }
}