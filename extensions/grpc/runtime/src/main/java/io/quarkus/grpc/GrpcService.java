package io.quarkus.grpc;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;
import javax.inject.Singleton;

/**
 * Stereotype used to mark a gRPC service class.
 */
@Singleton
@Stereotype
@Target(TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcService {

}
