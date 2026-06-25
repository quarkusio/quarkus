package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to force a class to be registered for reflection in native image mode.
 * Note that by default the class itself is registered including nested classes and interfaces,
 * but not the full class hierarchy. This can be changed by setting:
 * <ul>
 * <li>{@link #ignoreNested()} to true, to ignore nested classes.</li>
 * <li>{@link #registerFullHierarchy()} to true, to register the full hierarchy.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterForReflection {

    /**
     * If the methods should be registered
     */
    boolean methods() default true;

    /**
     * If the fields should be registered
     */
    boolean fields() default true;

    /**
     * If nested classes/interfaces should be ignored.
     * By default, nested classes are registered. To ignore them set it to true.
     */
    boolean ignoreNested() default false;

    /**
     * Alternative classes that should actually be registered for reflection instead of the current class.
     *
     * This allows for classes in 3rd party libraries to be registered without modification or writing an
     * extension. If this is set then the class it is placed on is not registered for reflection, so this should
     * generally just be placed on an empty class that is not otherwise used.
     */
    Class<?>[] targets() default {};

    /**
     * This allows for classes to be registered for reflection via class names. This is useful when it's necessary to
     * register private classes for Reflection.
     */
    String[] classNames() default {};

    boolean serialization() default false;

    boolean unsafeAllocated() default false;

    /**
     * The lambda capturing types performing serialization in the native image.
     *
     * @deprecated This parameter is no longer supported in reachability-metadata.json.
     *             Lambda serialization must be registered using @LambdaDescriptor,
     *             e.g.
     *             integration-tests/native-image-annotations/src/main/java/io/quarkus/it/nat/annotation/ProperLambdaHolder.java
     */
    @Deprecated(forRemoval = true, since = "3.36.0")
    String[] lambdaCapturingTypes() default {};

    /**
     * Lambda descriptors for serializable lambdas that need reflection metadata.
     * Each descriptor specifies the declaring class, method, and interfaces of a lambda.
     * <p>
     * e.g.
     *
     * <pre>
     * RegisterForReflection(
     *     lambdaDescriptors = {
     *         LambdaDescriptor(
     *             declaringClass = MyClass.class,
     *             declaringMethod = "getLambda",
     *             interfaces = { Function.class }
     *         )
     *     }
     * )
     * </pre>
     */
    LambdaDescriptor[] lambdaDescriptors() default {};

    /**
     * Whether the full class hierarchy and dependencies should be registered.
     * This is useful in order to use a class to be transfered through a RESTful service API.
     * <p>
     * In some cases, including nested classes might register classes you don't want to register. You can ignore nested classes
     * by setting {@link #ignoreNested()} to true.
     */
    boolean registerFullHierarchy() default false;
}
