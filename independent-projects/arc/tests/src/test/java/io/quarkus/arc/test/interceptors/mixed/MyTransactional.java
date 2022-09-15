package io.quarkus.arc.test.interceptors.mixed;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ TYPE, CONSTRUCTOR })
@Retention(RUNTIME)
@InterceptorBinding
public @interface MyTransactional {

}
