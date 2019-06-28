package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a config item should be converted using a default converter: built-in/implicit converters or a custom
 * converter.
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@Documented
public @interface DefaultConverter {
}
