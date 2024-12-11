package io.quarkus.opentelemetry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * Identifies that the current path should not be select for tracing.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Experimental("This annotation is only supported by Quarkus REST currently")
public @interface Traceless {
}
