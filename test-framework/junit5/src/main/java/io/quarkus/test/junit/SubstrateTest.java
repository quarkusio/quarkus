package io.quarkus.test.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation that indicates that this test should be run using a native image,
 * rather than in the JVM. This must also be combined with {@link QuarkusTestExtension}.
 *
 * The standard usage pattern is expected to be a base test class that runs the
 * tests using the JVM version of Quarkus, with a subclass that extends the base
 * test and is annotated with this annotation to perform the same checks against
 * the native image.
 *
 * Note that it is not possible to mix JVM and native image tests in the same test
 * run, it is expected that the JVM tests will be standard unit tests that are
 * executed by surefire, while the native image tests will be integration tests
 * executed by failsafe.
 * 
 * @deprecated Use {@link NativeImageTest} instead.
 *
 */
@Deprecated
@Target(ElementType.TYPE)
@ExtendWith({ DisabledOnSubstrateCondition.class, QuarkusTestExtension.class, NativeTestExtension.class })
@Retention(RetentionPolicy.RUNTIME)
public @interface SubstrateTest {
}
