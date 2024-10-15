package io.quarkus.opentelemetry.runtime.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies that the current path should not select for
 * adding trace headers.
 * <p>
 * Used together with {@code jakarta.ws.rs.Path} annotation.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Traceless {
}
