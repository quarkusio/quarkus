package io.quarkus.test.junit.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.condition.DisabledIfConfig.DisabledIfConfigs;

/**
 * {@code @DisabledIfConfig} is used to signal that the annotated test
 * class or test method is <em>disabled</em> if the value of the specified
 * {@linkplain #named application property} matches the specified
 * {@linkplain #matches regular expression}.
 *
 * <p>
 * When declared at the class level, the result will apply to all test methods
 * within that class as well.
 *
 * <p>
 * This annotation is not {@link java.lang.annotation.Inherited @Inherited}.
 * Consequently, if you wish to apply the same semantics to a subclass, this
 * annotation must be redeclared on the subclass.
 *
 * <p>
 * If a test method is disabled via this annotation, that prevents execution
 * of the test method and method-level lifecycle callbacks such as
 * {@code @BeforeEach} methods, {@code @AfterEach} methods, and corresponding
 * extension APIs. However, that does not prevent the test class from being
 * instantiated, and it does not prevent the execution of class-level lifecycle
 * callbacks such as {@code @BeforeAll} methods, {@code @AfterAll} methods, and
 * corresponding extension APIs.
 *
 * <p>
 * If the specified application property is undefined, the presence of this
 * annotation will have no effect on whether or not the class or method
 * is disabled.
 *
 * <p>
 * This annotation may be used as a meta-annotation in order to create a
 * custom <em>composed annotation</em> that inherits the semantics of this
 * annotation.
 *
 * <p>
 * This annotation is a {@linkplain Repeatable repeatable} annotation and may
 * be declared multiple times on an {@link java.lang.reflect.AnnotatedElement
 * AnnotatedElement} such as a test interface, test class, or test method.
 * Specifically, this annotation will be found if it is directly present,
 * indirectly present, or meta-present on a given element.
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(DisabledIfConfigs.class)
@ExtendWith(DisabledIfConfigCondition.class)
public @interface DisabledIfConfig {
    /**
     * Specifies the name of the application property that will be used
     * as a condition for enabling the annotated element.
     *
     * @return the name of the application property to be evaluated for conditional activation
     */
    String named();

    /**
     * A regular expression that will be used to match against the retrieved
     * value of the {@link #named} property.
     *
     * @return the regular expression; never <em>blank</em>
     * @see String#matches(String)
     * @see java.util.regex.Pattern
     */
    String matches();

    /**
     * Custom reason to provide if the test or container is disabled.
     *
     * <p>
     * If a custom reason is supplied, it will be combined with the default
     * reason for this annotation. If a custom reason is not supplied, the default
     * reason will be used.
     */
    String disabledReason() default "";

    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface DisabledIfConfigs {
        /**
         * An array of {@link DisabledIfConfig} annotations that are used
         * to specify conditional activation criteria based on application properties.
         */
        DisabledIfConfig[] value();
    }
}
