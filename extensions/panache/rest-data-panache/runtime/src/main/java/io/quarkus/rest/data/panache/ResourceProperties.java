package io.quarkus.rest.data.panache;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An optional annotation to customize generated JAX-RS resource.
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE })
public @interface ResourceProperties {

    /**
     * URL path segment that should be used to access the resources.
     * This path segment is prepended to the segments specified with the {@link MethodProperties} annotations used on this
     * resource's methods.
     * <p>
     * Default: hyphenated resource name without a suffix. Ignored suffixes are `Controller` and `Resource`.
     */
    String path() default "";

    /**
     * Return a paged collection from the methods that return collections.
     * Requested page number and size are extracted from the query parameters `page` (default 0) and `size` (default 20).
     * These additional headers are injected to the response: first, last, prev (if exists), next (if exists).
     * <p>
     * Default: true.
     */
    boolean paged() default true;

    /**
     * Generate operations that support HAL content type.
     * HAL methods are generated in addition to the standard methods. They accept the same parameters but return a content of
     * `application/hal+json` type.
     * The operations that support HAL responses are `get`, `list`, `add` and `update`.
     * <p>
     * Default: false.
     */
    boolean hal() default false;

    /**
     * Name that should be used then generating a HAL collection response.
     * <p>
     * Default: hyphenated resource name without a suffix. Ignored suffixes are `Controller` and `Resource`.
     */
    String halCollectionName() default "";
}
