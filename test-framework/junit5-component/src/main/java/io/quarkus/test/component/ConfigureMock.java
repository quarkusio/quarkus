package io.quarkus.test.component;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field of a test class as a target of a mock dependency injection.
 */
@Retention(RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigureMock {

}
