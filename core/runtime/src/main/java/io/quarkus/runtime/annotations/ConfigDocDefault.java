package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.config.ConfigMapping;

/**
 * Specifies a default value for the documentation.
 * <p>
 * Replaces defaultValueForDocumentation for the {@link ConfigMapping} approach.
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, METHOD })
public @interface ConfigDocDefault {

    String value();
}
