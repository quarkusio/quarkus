package io.quarkus.test.junit.main;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for {@link QuarkusMainTest} and {@link QuarkusMainIntegrationTest} used to
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Launch {
    /**
     * The program arguments to launch with
     */
    String[] value();

    /**
     * Expected return code
     */
    int exitCode() default 0;
}
