package io.quarkus.test.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @DisabledOnIntegrationTest} is used to signal that the annotated test class or
 * test method should not be executed for as part of a {@link QuarkusIntegrationTest}.
 *
 * <p>
 * {@code @DisabledOnIntegrationTest} may optionally be declared with a {@linkplain #value
 * reason} to document why the annotated test class or test method is disabled.
 *
 * <p>
 * When applied at the class level, all test methods within that class
 * are automatically disabled.
 *
 * <p>
 * When applied at the method level, the presence of this annotation does not
 * prevent the test class from being instantiated. Rather, it prevents the
 * execution of the test method and method-level lifecycle callbacks such as
 * {@code @BeforeEach} methods, {@code @AfterEach} methods, and corresponding
 * extension APIs.
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DisabledOnIntegrationTest {
    /**
     * Reason for disabling this test
     */
    String value() default "";
}
