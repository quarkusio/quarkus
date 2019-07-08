package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Deprecated, use {@link Recorder} instead.
 *
 * This will be retained until at least Quarkus 0.21.0 to allow time for 3rd party extensions to update.
 * It will be removed at some point after that.
 *
 * @see Recorder
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated
public @interface Template {
}
