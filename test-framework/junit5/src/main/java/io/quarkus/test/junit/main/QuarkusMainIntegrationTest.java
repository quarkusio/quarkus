package io.quarkus.test.junit.main;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusMainIntegrationTestExtension;

/**
 * Annotation that indicates that this test should be run on the result of the Quarkus build.
 * That means that if a jar was created, that jar is launched using {@code java -jar ...}
 * (and thus runs in a separate JVM than the test).
 * If instead a native image was created, the that image is launched.
 * Finally, if a container image was created during the build, then a new container is created and run.
 *
 * The standard usage pattern is expected to be a base test class that runs the
 * tests using the JVM version of Quarkus, with a subclass that extends the base
 * test and is annotated with this annotation to perform the same checks against
 * the native image.
 *
 * Injection of beans into a test class using {@code @Inject} is not supported
 * in {@code QuarkusMainIntegrationTest}.
 */
@Target(ElementType.TYPE)
@ExtendWith({ QuarkusMainIntegrationTestExtension.class })
@Retention(RetentionPolicy.RUNTIME)
public @interface QuarkusMainIntegrationTest {

}
