package io.quarkus.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to be used on a {@link java.nio.file.Path} field inside a test
 * to get the log file injected into the test.
 *
 * The Path will field only be set if the application has actually been started with a logfile configured.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LogFile {
}
