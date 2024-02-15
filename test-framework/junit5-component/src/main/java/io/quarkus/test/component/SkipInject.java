package io.quarkus.test.component;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Instructs the test engine to skip injection for test method parameters.
 * <p>
 * If declared on a parameter of a test method then skip injection for this specific parameter.
 * <p>
 * If declared on a test method then skip injection for all parameters.
 */
@Retention(RUNTIME)
@Target({ PARAMETER, METHOD })
public @interface SkipInject {

}
