package io.quarkus.arc.test.interceptors.mixed;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.interceptor.InterceptorBinding;

@Target({ TYPE, CONSTRUCTOR })
@Retention(RUNTIME)
@InterceptorBinding
public @interface MyTransactional {

}
