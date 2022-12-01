package io.quarkus.grpc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Qualifies a gRPC service.
 */
@Qualifier
@Target({ FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
public @interface GrpcService {

}
