package io.quarkus.test.timeouts;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fails the annotated test (or every test of the annotated class) if it runs for longer than the shared
 * {@linkplain TestTimeouts#get() test timeout}. This is the configurable counterpart of JUnit's {@code @Timeout}, whose
 * value has to be a constant.
 * <p>
 * The test thread is interrupted when the limit is reached, the same way as JUnit's {@code @Timeout} does.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface TimeLimit {

    /**
     * Allows tests that perform many sequential waits to run for a multiple of the shared timeout.
     */
    int multiplier() default 1;
}
