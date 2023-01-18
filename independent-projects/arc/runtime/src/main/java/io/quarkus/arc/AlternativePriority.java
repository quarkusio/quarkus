package io.quarkus.arc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.inject.Alternative;

/**
 * If a bean is annotated with this annotation, it is considered an enabled alternative with given priority.
 * Effectively, this is a shortcut for {@code Alternative} plus {@code Priority} annotations.
 *
 * This annotation can be used not only on bean classes, but also method and field producers (unlike pure {@code Priority}).
 *
 * @deprecated Use {@link Alternative} and {@link io.quarkus.arc.Priority}/{@link jakarta.annotation.Priority} instead
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
@Deprecated
public @interface AlternativePriority {
    /**
     * The priority value of this alternative
     */
    int value();
}
