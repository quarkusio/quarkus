package io.quarkus.quickcli.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * CDI qualifier marking the top-level command for the QuickCLI CommandLine instance.
 * Can be used together with {@link jakarta.inject.Named} — use the runtime property
 * {@code quarkus.quickcli.top-command} to specify which Named TopCommand to use.
 */
@Qualifier
@Target({ FIELD, PARAMETER, TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TopCommand {
}
