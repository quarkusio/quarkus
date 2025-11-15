package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to register resource files to be included in the native image.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RegisterResources.List.class)
public @interface RegisterResources {

    /**
     * Add an array of glob patterns for matching resource paths that should be added to the native image.
     * <p>
     * Use slash ({@code /}) as a path separator on all platforms. Globs must not start with slash.
     */
    String[] globs() default {};

    /**
     * The repeatable holder for {@link RegisterResources}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface List {
        /**
         * The {@link RegisterResources} instances.
         *
         * @return the instances
         */
        RegisterResources[] value();
    }
}
