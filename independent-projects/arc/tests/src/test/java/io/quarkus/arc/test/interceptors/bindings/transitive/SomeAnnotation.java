package io.quarkus.arc.test.interceptors.bindings.transitive;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.interceptor.InterceptorBinding;

/**
 * This annotation is a not a binding itself but a meta-annotation containing one.
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Documented
@CounterBinding
@InterceptorBinding
public @interface SomeAnnotation {
}
