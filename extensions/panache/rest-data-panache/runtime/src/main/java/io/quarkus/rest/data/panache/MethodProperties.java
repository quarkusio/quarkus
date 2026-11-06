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
     * Expose this operation as a JAX-RS endpoint.
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

    /**
     * List of the security roles permitted to access this operation.
     * <p>
     * When left empty, the roles declared with {@link ResourceProperties#rolesAllowed()} on the resource are inherited
     * by this operation. Set this attribute to override the resource-level roles for this operation only.
     * <p>
     * Default: "" (the resource-level roles are inherited)
     */
    String[] rolesAllowed() default {};
}
