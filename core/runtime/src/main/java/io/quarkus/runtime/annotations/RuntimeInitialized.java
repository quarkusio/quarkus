package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to force a class to be initialized at runtime
 * when using native image mode. This means that any static init code will be run
 * at startup, rather than when the image is built.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RuntimeInitialized {

    /**
     * Alternative classes that should actually be initialized at runtime instead of the current class.
     *
     * This allows for classes in 3rd party libraries to be initialized at runtime without modification
     * or writing an extension. If this is set then the class it is placed on is not initialized at runtime,
     * so this should generally just be placed on an empty class that is not otherwise used.
     */
    Class<?>[] targets() default {};
}
