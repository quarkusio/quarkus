package io.quarkus.arc.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * When applied to a field of class annotated with {@link ConfigProperties}, that field will be ignored
 * for the purposes of configuration
 *
 * @deprecated Please, use {@link io.smallrye.config.ConfigMapping} instead. This will be removed in a future Quarkus
 *             version.
 */
@Deprecated
@Target(FIELD)
@Retention(RUNTIME)
public @interface ConfigIgnore {

}
