package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given recorded parameter should have relaxed validation.
 * 
 * Normally if a field cannot be serialized to bytecode an exception will be thrown,
 * if this annotation is present the field is simply ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RelaxedValidation {
}
