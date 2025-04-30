package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A marker indicating a user-friendly documentation key for the {@link java.util.Map} type.
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, METHOD })
public @interface ConfigDocMapKey {
    String value();
}
