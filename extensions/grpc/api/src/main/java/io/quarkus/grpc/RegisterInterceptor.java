package io.quarkus.grpc;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.grpc.ServerInterceptor;

/**
 * Registers a {@link ServerInterceptor} for a particular gRPC service.
 *
 * @see GlobalInterceptor
 */
@Repeatable(RegisterInterceptors.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface RegisterInterceptor {
    Class<? extends ServerInterceptor> value();
}
