package io.quarkus.test.junit.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Used to signal that a test class or method should only be enabled if Semeru is used as the JVM runtime.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledOnSemeruCondition.class)
public @interface EnabledOnSemeru {

    String reason() default "";
}
