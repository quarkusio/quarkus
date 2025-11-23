package io.quarkus.test.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Used to signal that a test class or method should be disabled if Semeru is used as the JVM runtime.
 * <p>
 * We cannot test for Semeru exactly but we check the java.vendor is IBM Corporation.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnSemeruCondition.class)
public @interface DisabledOnSemeru {

    int versionGreaterThanOrEqualTo() default 0;

    int versionLessThanOrEqualTo() default 0;

    String reason() default "";
}
