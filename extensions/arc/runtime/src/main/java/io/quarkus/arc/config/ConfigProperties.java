package io.quarkus.arc.config;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Allow configuration properties with a common prefix to be grouped into a single class
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface ConfigProperties {

    String UNSET_PREFIX = "<< unset >>";

    /**
     * If the default is used, the class name will be used to determine the proper prefix
     */
    String prefix() default UNSET_PREFIX;
}
