package io.quarkus.grpc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes a {@link io.grpc.ServerInterceptor} that should be registered for all gRPC services, or a
 * {@link io.grpc.ClientInterceptor} that should be registered for all injected gRPC clients.
 *
 * @see RegisterInterceptor
 * @see RegisterClientInterceptor
 */
@Target({ FIELD, PARAMETER, TYPE, METHOD })
@Retention(RUNTIME)
public @interface GlobalInterceptor {
}
