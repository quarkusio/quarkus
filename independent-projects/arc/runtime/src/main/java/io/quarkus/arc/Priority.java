package io.quarkus.arc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation has the same semantics as {@link jakarta.annotation.Priority}.
 * <p>
 * Prior to Common Annotations 2.1, the {@code jakarta.annotation.Priority} annotation
 * was meta-annotated {@code @Target({TYPE, PARAMETER})} and so was only usable on class
 * declarations and method parameters. This annotation was introduced to allow annotating
 * producer methods and fields.
 * <p>
 * Since Common Annotations 2.1, the {@code jakarta.annotation.Priority} is no longer
 * meta-annotated {@code @Target}, so these two annotations are equivalent.
 * <p>
 * A priority specified by {@link jakarta.annotation.Priority} takes precedence.
 *
 * @deprecated use {@link jakarta.annotation.Priority}; this annotation will be removed at some time after Quarkus 3.6
 */
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface Priority {

    int value();

}
