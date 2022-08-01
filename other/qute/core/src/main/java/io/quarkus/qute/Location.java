package io.quarkus.qute;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Qualifies an injected template. The {@link #value()} is used to locate the template; it represents the path relative from
 * the base path.
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, METHOD })
public @interface Location {

    /**
     * @return the path relative from the base path, must be a non-empty string
     */
    String value();

}