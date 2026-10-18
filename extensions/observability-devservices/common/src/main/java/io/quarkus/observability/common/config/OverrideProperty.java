package io.quarkus.observability.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Override the property in the value,
 * with the value of the annotated method's return.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface OverrideProperty {
    /**
     * The property key to override.
     *
     * @return the property key
     */
    String value();
}
