package io.quarkus.grpc;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;
import javax.inject.Singleton;

import io.quarkus.grpc.runtime.supports.context.GrpcEnableRequestContext;

/**
 * Stereotype used to mark a gRPC service class.
 */
@Singleton
@GrpcEnableRequestContext
@Stereotype
@Target(TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcService {

}
