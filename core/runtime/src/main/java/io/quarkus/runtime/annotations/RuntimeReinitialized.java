package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to force a class to be reinitialized at runtime
 * when using native image mode. This means that any static init code will be run
 * twice, once when the image is built, and once when the application starts.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RuntimeReinitialized {

    /**
     * Alternative classes that should actually be runtime reinitialized instead of the current class.
     *
     * This allows for classes in 3rd party libraries to be reinitialized without modification or writing an
     * extension. If this is set then the class it is placed on is not reinitialized, so this should
     * generally just be placed on an empty class that is not otherwise used.
     */
    Class<?>[] targets() default {};
}
