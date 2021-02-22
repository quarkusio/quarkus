package io.quarkus.arc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If a bean is annotated with this annotation, it is considered an enabled alternative with given priority.
 * Effectively, this is a shortcut for {@code Alternative} plus {@code Priority} annotations.
 *
 * This annotation can be used not only on bean classes, but also method and field producers (unlike pure {@code Priority}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
public @interface AlternativePriority {
    /**
     * The priority value of this alternative
     */
    int value();
}
