package io.quarkus.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on classes that are meant to be used as Jackson mixins.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JacksonMixin {

    /**
     * The types for which the mixin should apply to.
     */
    Class<?>[] value();
}
