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
 * As of Quarkus 3.16, the default behavior of the annotation (meaning that {@code scope} has not been set)
 * is that test classes annotated with the same {@code WithTestResource} will <strong>not</strong> force a restart
 * of Quarkus.
 * <p>
 * The equivalent behavior to {@code QuarkusTestResource(restrictToAnnotatedClass = false)} is to use
 * {@code WithTestResource(scope = TestResourceScope.GLOBAL)},
 * while the equivalent behavior to {@code QuarkusTestResource(restrictToAnnotatedClass = true)} is to use
 * {@code WithTestResource(scope = TestResourceScope.RESTRICTED_TO_CLASS)},
 * <p>
 * WARNING: this annotation, introduced in 3.13, caused some issues so it was decided to undeprecate
 * {@link QuarkusTestResource} and rework the behavior of this annotation. For now, we recommend not using it
 * until we improve its behavior.
 * <p>
 * Note: When using the {@code scope=TestResourceScope.RESTRICTED_TO_CLASS}, each test that is annotated
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
     * Defines how Quarkus behaves with regard to the application of the resource to this test and the test-suite in general.
     * The default is {@link TestResourceScope#MATCHING_RESOURCES} which means that if two tests are annotated with the same
     * {@link WithTestResource} annotation, no restart will take place between tests.
     */
    TestResourceScope scope() default TestResourceScope.MATCHING_RESOURCES;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        WithTestResource[] value();
    }
}
