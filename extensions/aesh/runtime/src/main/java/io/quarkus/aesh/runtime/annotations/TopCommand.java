package io.quarkus.aesh.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Top-level command for Aesh CommandRunner instance. This qualifier can be used together with {@link jakarta.inject.Named}.
 * Runtime property <i>quarkus.aesh.top-command</i> should be used to specify which Named TopCommand will be used.
 */
@Qualifier
@Target({ FIELD, PARAMETER, TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TopCommand {
}
