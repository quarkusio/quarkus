package io.quarkus.test.junit.main;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusMainTestExtension;

/**
 * Annotation that indicates that this test should run the Quarkus main method
 * inside the current JVM. A new in-memory quarkus application will be generated,
 * and will be run to completion.
 *
 * Injection of beans into a test class using {@code @Inject} is not supported
 * in {@code QuarkusMainTest}.
 *
 * Note that this can be used in conjunction with other {@link io.quarkus.test.junit.QuarkusTest}
 * based tests. {@code QuarkusMainTest} is used to check a complete execution, while {@code QuarkusTest} can be
 * used to inject components and perform more fine-grained checks.
 */
@Target(ElementType.TYPE)
@ExtendWith({ QuarkusMainTestExtension.class })
@Retention(RetentionPolicy.RUNTIME)
public @interface QuarkusMainTest {

}
