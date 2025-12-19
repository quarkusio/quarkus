package io.quarkus.arc.test.decorators.interceptor.other;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Documented
@InterceptorBinding
public @interface Logging {

}
