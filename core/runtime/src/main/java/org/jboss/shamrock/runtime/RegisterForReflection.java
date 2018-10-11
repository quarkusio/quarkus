package org.jboss.shamrock.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to force a class to be registered for reflection in native image mode
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterForReflection {

    /**
     * If the methods should be registered
     */
    boolean methods() default true;

    /**
     * If the fields should be registered
     */
    boolean fields() default true;
}
