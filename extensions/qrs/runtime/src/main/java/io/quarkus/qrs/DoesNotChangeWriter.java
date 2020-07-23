package io.quarkus.qrs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a response filter does not change the writer select at build time
 */
//FIXME: better name?
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface DoesNotChangeWriter {
}
