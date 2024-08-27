package io.quarkus.test.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import io.quarkus.test.common.WithTestResource.List;

/**
 * Used to define a test resource, which can affect various aspects of the application lifecycle.
 * <p>
 * WARNING: this annotation, introduced in 3.13, caused some issues so it was decided to undeprecate
 * {@link QuarkusTestResource} and rework the behavior of this annotation. For now, we recommend not using it
 * until we improve its behavior.
 * <p>
 * Note: When using the {@code restrictToAnnotatedClass=true} (which is the default), each test that is annotated
 * with {@code @WithTestResource} will result in the application being re-augmented and restarted (in a similar fashion
 * as happens in dev-mode when a change is detected) in order to incorporate the settings configured by the annotation.
 * If there are many instances of the annotation used throughout the testsuite, this could result in slow test execution.
 * <p>
 * <b>All</b> {@code WithTestResource} annotations in the test module
 * are discovered (regardless of the test which contains the annotation)
 * and their corresponding {@code QuarkusTestResourceLifecycleManager}
 * started <b>before</b> <b>any</b> test is run.
 * <p>
 * Note that test resources are never restarted when running {@code @Nested} test classes.
 * <p>
 * The only difference with {@link QuarkusTestResource} is that the default value for
 * {@link #restrictToAnnotatedClass()} {@code == true}.
 * </p>
 * <p>
 * This means that any resources managed by {@link #value()} apply to an individual test class or test profile, unlike with
 * {@link QuarkusTestResource} where a resource applies to all test classes.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(List.class)
public @interface WithTestResource {

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
     * Note: When this is set to {@code true} (which is the default), the annotation {@code @WithTestResource} will result
     * in the application being re-augmented and restarted (in a similar fashion as happens in dev-mode when a change is
     * detected) in order to incorporate the settings configured by the annotation.
     */
    boolean restrictToAnnotatedClass() default true;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        WithTestResource[] value();
    }
}
