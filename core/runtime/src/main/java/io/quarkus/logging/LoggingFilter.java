package io.quarkus.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes the filter class known to Quarkus by the specified name.
 * The filter can then be configured for a handler (like the logging handler using {@code quarkus.log.console.filter}).
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LoggingFilter {

    /**
     * Name with which the filter is referred to in configuration
     */
    String name();
}
