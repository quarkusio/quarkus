package io.quarkus.arc;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation has the same semantics as {@link jakarta.annotation.Priority} except that the {@link Target} meta-annotation
 * is
 * not present. The main motivation is to support method and field declarations, i.e. this annotation can be used for producer
 * methods and fields. Note that this problem is fixed in Common Annotations 2.1.
 * <p>
 * A priority specified by {@link AlternativePriority} and {@link jakarta.annotation.Priority} takes precedence.
 */
@Retention(RUNTIME)
public @interface Priority {

    int value();

}
