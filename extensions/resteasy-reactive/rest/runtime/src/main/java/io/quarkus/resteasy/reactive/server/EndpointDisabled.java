package io.quarkus.resteasy.reactive.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides the ability to conditionally enable a JAX-RS Resource class at runtime based on the value of a property.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface EndpointDisabled {

    /**
     * Name of the property to check
     */
    String name();

    /**
     * Expected {@code String} value of the property (specified by {@code name}) if the Resource class is to be disabled
     */
    String stringValue();

    /**
     * Determines if the Resource class is to be disabled when the property name specified by {@code name} has not been
     * specified at all
     */
    boolean disableIfMissing() default false;
}
