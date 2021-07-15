package io.quarkus.test.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Manage the lifecycle of a test resource, for instance a H2 test server.
 * <p>
 * These resources are started before the first test is run, and are closed
 * at the end of the test suite. They are configured via the {@link QuarkusTestResource}
 * annotation, which can be placed on any class in the test suite.
 *
 * These can also be loaded via a service loader mechanism, however if a service
 * loader is used it should not also be annotated as this will result in it being executed
 * twice.
 *
 * Note that when using these with QuarkusUnitTest (rather than @QuarkusTest) they run
 * before the ClassLoader has been setup. This means injection may not work
 * as expected.
 * <p>
 * Due to the very early execution in the test setup lifecycle, logging does <b>not</b>
 * work in such a manager.
 */
public interface QuarkusTestResourceLifecycleManager {

    /**
     * Start the test resource.
     *
     * @return A map of system properties that should be set for the running test
     */
    Map<String, String> start();

    /**
     * Stop the test resource.
     */
    void stop();

    /**
     * Arguments passed to the lifecycle manager before it starts
     * These arguments are taken from {@code QuarkusTestResource#initArgs()}
     *
     * The {@code args} is never null
     *
     * @see QuarkusTestResource#initArgs()
     */
    default void init(Map<String, String> initArgs) {

    }

    /**
     * Allow each resource to provide custom injection of fields of the test class.
     *
     * Most implementations will likely use {@link QuarkusTestResourceLifecycleManager#inject(TestInjector)}
     * as it provides a simpler way to inject into fields of tests.
     *
     * It is worth mentioning that this injection into the test class is not under the control of CDI and happens after CDI has
     * performed
     * any necessary injections into the test class.
     */
    default void inject(Object testInstance) {

    }

    /**
     * Simplifies the injection of fields of the test class by providing methods to handle the common injection cases.
     *
     * In situations not covered by {@link TestInjector}, user can resort to implementing
     * {@link QuarkusTestResourceLifecycleManager#inject(Object)}
     *
     * It is worth mentioning that this injection into the test class is not under the control of CDI and happens after CDI has
     * performed
     * any necessary injections into the test class.
     */
    default void inject(TestInjector testInjector) {

    }

    /**
     * If multiple Test Resources are specified,
     * this control the order of which they will be executed.
     *
     * @return The order to be executed. The larger the number, the later the Resource is invoked.
     */
    default int order() {
        return 0;
    }

    /**
     * Provides methods to handle the common injection cases. See
     * {@link QuarkusTestResourceLifecycleManager#inject(TestInjector)}
     */
    interface TestInjector {

        /**
         * @param fieldValue The actual value to inject into a test field
         * @param predicate User supplied predicate which can be used to determine whether or not the field should be
         *        set with with {@code fieldValue}
         */
        void injectIntoFields(Object fieldValue, Predicate<Field> predicate);

        /**
         * Returns {@code true} if the field is annotated with the supplied annotation.
         */
        class Annotated implements Predicate<Field> {

            private final Class<? extends Annotation> annotationClass;

            public Annotated(Class<? extends Annotation> annotationClass) {
                this.annotationClass = annotationClass;
            }

            @Override
            public boolean test(Field field) {
                return field.getAnnotation(annotationClass) != null;
            }
        }

        /**
         * Returns {@code true} if the field is annotated with the supplied annotation and can also be assigned
         * to the supplied type.
         */
        class AnnotatedAndMatchesType implements Predicate<Field> {

            private final Class<? extends Annotation> annotationClass;
            private final Class<?> expectedFieldType;

            public AnnotatedAndMatchesType(Class<? extends Annotation> annotationClass, Class<?> expectedFieldType) {
                this.annotationClass = annotationClass;
                this.expectedFieldType = expectedFieldType;
            }

            @Override
            public boolean test(Field field) {
                if (field.getAnnotation(annotationClass) == null) {
                    return false;
                }
                return field.getType().isAssignableFrom(expectedFieldType);
            }
        }
    }
}
