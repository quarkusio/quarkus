package io.quarkus.panache.rest.common;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target({ METHOD })
public @interface OperationProperties {

    /**
     * Expose this operations as a JAX-RS endpoint.
     *
     * Default: true
     */
    boolean exposed() default true;

    /**
     * URL path segment that should be used to access this operation.
     *
     * Default: ""
     */
    String path() default "";
}
