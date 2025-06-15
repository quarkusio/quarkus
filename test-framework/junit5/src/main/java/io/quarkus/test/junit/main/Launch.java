package io.quarkus.test.junit.main;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for {@link QuarkusMainTest} and {@link QuarkusMainIntegrationTest} that is used to launch command line
 * applications. The annotation is meant to be used on test methods only. See also {@link LaunchResult} and
 * {@link QuarkusMainLauncher}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Launch {
    /**
     * The program arguments to launch with
     */
    String[] value() default "";

    /**
     * Expected return code
     */
    int exitCode() default 0;
}
