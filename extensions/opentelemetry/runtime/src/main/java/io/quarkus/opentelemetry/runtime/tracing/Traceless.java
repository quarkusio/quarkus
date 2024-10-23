package io.quarkus.opentelemetry.runtime.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies that the current path should not be select for tracing.
 * <p>
 * Used together with {@code jakarta.ws.rs.Path} annotation.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Traceless {
}
