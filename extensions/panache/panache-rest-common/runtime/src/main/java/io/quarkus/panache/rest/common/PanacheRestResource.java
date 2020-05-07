package io.quarkus.panache.rest.common;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface PanacheRestResource {

    /**
     * Whether a particular method or all controller methods should be exposed as JAX-RS resources.
     * For example, hide a `delete` method if it shouldn't be exposed via REST API.
     *
     * Default: true.
     */
    boolean exposed() default true;

    /**
     * Whether HAL version of a method or all controller methods should be generated.
     * HAL methods are generated in addition to the standard methods. They accept the same parameters but return a content of
     * `application/hal+json` type.
     * The methods that support HAL responses are `get`, `list`, `add` and `update`.
     *
     * Default: false.
     */
    boolean hal() default false;
}
