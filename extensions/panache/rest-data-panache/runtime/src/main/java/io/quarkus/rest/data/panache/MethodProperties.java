package io.quarkus.rest.data.panache;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An optional annotation to customize generated JAX-RS resource methods.
 */
@Documented
@Retention(RUNTIME)
@Target({ METHOD })
public @interface MethodProperties {

    /**
     * Expose this operations as a JAX-RS endpoint.
     * <p>
     * Default: true
     */
    boolean exposed() default true;

    /**
     * URL path segment that should be used to access this operation.
     * This path segment is appended to the segment specified with the {@link ResourceProperties} annotation used on this
     * resource.
     * <p>
     * Default: ""
     */
    String path() default "";
}
