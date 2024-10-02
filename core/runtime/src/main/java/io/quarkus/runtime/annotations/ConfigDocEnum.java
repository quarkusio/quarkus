package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Provides a way to configure how an enum is handled.
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
@Documented
public @interface ConfigDocEnum {

    /**
     * This can be used to enforce hyphenating the enum values even if a converter is present.
     */
    boolean enforceHyphenateValues() default false;

}
