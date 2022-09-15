package io.quarkus.arc.test.interceptors.noclassinterceptors;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ TYPE, METHOD, CONSTRUCTOR })
@Retention(RUNTIME)
@Documented
@InterceptorBinding
public @interface ClassLevel {
}
