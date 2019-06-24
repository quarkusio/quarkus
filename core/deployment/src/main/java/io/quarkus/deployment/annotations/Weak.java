package io.quarkus.deployment.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that the given produced item is produced only weakly. If a build only consumes items produced weakly
 * by a step, the step is not included. If applied to a method, the return value of the method is considered "weak".
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
public @interface Weak {
}
