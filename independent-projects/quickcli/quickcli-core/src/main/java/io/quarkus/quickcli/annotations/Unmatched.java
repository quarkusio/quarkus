package io.quarkus.quickcli.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code List<String>} field to capture any unmatched command-line arguments.
 * When present, the parser will not throw an error for unknown options or arguments;
 * instead they are collected into this field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Unmatched {
}
