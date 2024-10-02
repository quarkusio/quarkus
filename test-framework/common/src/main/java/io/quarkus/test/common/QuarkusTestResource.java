package io.quarkus.test.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource.List;

/**
 * Used to define a test resource.
 * <p>
 * <b>All</b> {@code QuarkusTestResource} annotations in the test module
 * are discovered (regardless of the test which contains the annotation)
 * and their corresponding {@code QuarkusTestResourceLifecycleManager}
 * started <b>before</b> <b>any</b> test is run.
 * <p>
 * Note that test resources are never restarted when running {@code @Nested} test classes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(List.class)
public @interface QuarkusTestResource {

    /**
     * @return The class managing the lifecycle of the test resource.
     */
    Class<? extends QuarkusTestResourceLifecycleManager> value();

    /**
     * @return The arguments to be passed to the {@code QuarkusTestResourceLifecycleManager}
     *
     * @see QuarkusTestResourceLifecycleManager#init(Map)
     */
    ResourceArg[] initArgs() default {};

    /**
     * Whether this test resource is to be started in parallel (concurrently) along with others also marked as parallel
     */
    boolean parallel() default false;

    /**
     * Whether this annotation should only be enabled if it is placed on the currently running test class or test profile.
     * Note that this defaults to true for meta-annotations since meta-annotations are only considered
     * for the current test class or test profile.
     * <p>
     * Note: When this is set to {@code true}, the annotation {@code @QuarkusTestResource} will result
     * in the application being re-augmented and restarted (in a similar fashion as happens in dev mode when a change is
     * detected).
     */
    boolean restrictToAnnotatedClass() default false;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        QuarkusTestResource[] value();
    }
}
