package io.quarkus.grpc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes a ServerInterceptor that should be registered for all gRPC services
 *
 * @see RegisterInterceptor
 */
@Target({ FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
public @interface GlobalInterceptor {
}
