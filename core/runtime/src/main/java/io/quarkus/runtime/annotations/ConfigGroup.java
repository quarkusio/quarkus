package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An optional marker indicating that the configuration member is a nested element of a
 * {@link io.quarkus.runtime.annotations.ConfigRoot}.
 * <p>
 * This annotation can only be used on interfaces.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigGroup {
}
