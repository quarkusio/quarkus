package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to register resource files to be included in the native image.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterResources {

    /**
     * Add an array of glob patterns for matching resource paths that should be added to the native image.
     * <p>
     * Use slash ({@code /}) as a path separator on all platforms. Globs must not start with slash.
     */
    String[] includeGlobs() default {};

    /**
     * Add an array of regular expressions for matching resource paths that should be added to the native image.
     * <p>
     * Use slash ({@code /}) as a path separator on all platforms. The patterns must not start with slash.
     */
    String[] includePatterns() default {};

    /**
     * Add an array of glob patterns for matching resource paths that should <strong>not</strong> be added to the
     * native image.
     * <p>
     * Use slash ({@code /}) as a path separator on all platforms. Globs must not start with slash.
     */
    String[] excludeGlobs() default {};

    /**
     * Add an array of regular expressions for matching resource paths that should <strong>not</strong> be added
     * to the native image.
     * <p>
     * Use slash ({@code /}) as a path separator on all platforms. The pattern must not start with slash.
     */
    String[] excludePatterns() default {};
}