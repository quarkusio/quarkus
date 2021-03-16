package io.quarkus.arc.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Uses in order to set the prefix for a configuration object at the injection point
 */
@Qualifier
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface ConfigPrefix {

    /**
     * The common prefix that properties will use
     */
    String value();
}
