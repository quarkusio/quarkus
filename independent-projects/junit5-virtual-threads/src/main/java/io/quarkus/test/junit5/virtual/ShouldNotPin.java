package io.quarkus.test.junit5.virtual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker indicating that the test method or class should not pin the carrier thread.
 * If, during the execution of the test, a virtual thread pins the carrier thread, the test fails.
 * However, occasional pin can still be allowed by setting {@code atMost} value
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
public @interface ShouldNotPin {

    /**
     * Set value to allow occasional pin events
     *
     * @return
     */
    int atMost() default 0;
}
