package io.quarkus.runtime.annotations;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicate that the given item is a configuration root. Instances of classes with this annotation will
 * be made available to build steps or run time recorders, according to the {@linkplain #phase() phase} of the
 * value.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
public @interface ConfigRoot {
    /**
     * Determine the prefix key of the configuration root.
     *
     * @return the prefix key name
     */
    String prefix() default "quarkus";

    /**
     * Determine the phase of this configuration root.
     *
     * @return the phase
     */
    ConfigPhase phase() default BUILD_TIME;

    /**
     * Determine the base key of the configuration root.
     *
     * @return the base key name
     */
    String name() default ConfigItem.HYPHENATED_ELEMENT_NAME;
}
