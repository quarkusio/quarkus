package io.quarkus.grpc.runtime.devmode;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * 
 * @see StreamCollectorInterceptor
 */
@InterceptorBinding
@Target({ TYPE })
@Retention(RUNTIME)
public @interface CollectStreams {

}
