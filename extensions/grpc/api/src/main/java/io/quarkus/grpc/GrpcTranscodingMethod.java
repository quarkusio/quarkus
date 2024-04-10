package io.quarkus.grpc;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcTranscodingMethod {

    String grpcMethodName();

    String httpMethod();

    String httpPath();
}
