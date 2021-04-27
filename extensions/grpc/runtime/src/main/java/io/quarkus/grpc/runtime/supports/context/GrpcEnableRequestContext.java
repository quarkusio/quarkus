package io.quarkus.grpc.runtime.supports.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

@Inherited
@InterceptorBinding
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcEnableRequestContext {
}
