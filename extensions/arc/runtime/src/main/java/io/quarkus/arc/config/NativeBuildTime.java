package io.quarkus.arc.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A config property injected during the static initialization phase of a native image build may result in unexpected errors
 * because the injected value was obtained at build time and cannot be updated at runtime.
 * <p>
 * If it's intentional and expected then use this annotation to eliminate the false positive.
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
public @interface NativeBuildTime {

}
