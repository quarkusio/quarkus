package io.quarkus.test.common;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to indicate the type of the <em>repeatable annotation
 * type</em> annotated with a {@code QuarkusTestResource} annotations.
 *
 * <p>
 *
 * @deprecated Use {@link WithTestResourceRepeatable} instead
 *             </p>
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated(forRemoval = true)
public @interface QuarkusTestResourceRepeatable {

    /**
     * @return The class annotated with a {@code QuarkusTestResource} annotations.
     */
    Class<? extends Annotation> value();
}
