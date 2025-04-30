package io.quarkus.arc.runtime;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * Interceptor binding for {@link ConfigStaticInitCheckInterceptor}.
 */
@InterceptorBinding
@Retention(RUNTIME)
@Target(TYPE)
public @interface ConfigStaticInitCheck {

}
