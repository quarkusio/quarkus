package io.quarkus.test.junit5.virtual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the method or class can pin. At most can be set to indicate the maximum number of events.
 * If, during the execution of the test, a virtual thread does not pin the carrier thread, or pins it more than
 * the given {@code atMost} value, the test fails.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
public @interface ShouldPin {
    int atMost() default Integer.MAX_VALUE;

}
