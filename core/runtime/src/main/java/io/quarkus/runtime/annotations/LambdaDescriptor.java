package io.quarkus.runtime.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describes a serializable lambda for reflection metadata generation.
 * Used within {@link RegisterForReflection#lambdaDescriptors()}.
 * <p>
 * This generates the reachability-metadata.json format for lambda serialization support.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LambdaDescriptor {

    /**
     * The class that declares the method containing the lambda.
     */
    Class<?> declaringClass();

    /**
     * The name of the method that contains the lambda expression.
     */
    String declaringMethod();

    /**
     * The parameter types of the declaring method.
     * Use empty array for methods with no parameters.
     */
    Class<?>[] parameterTypes() default {};

    /**
     * The interfaces implemented by the lambda, e.g. Function.class, Comparator.class etc.
     */
    Class<?>[] interfaces();
}
