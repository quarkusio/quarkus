package io.quarkus.mongodb.panache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated use {@link io.quarkus.mongodb.panache.common.ProjectionFor} instead.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated(forRemoval = true, since = "2.1.0")
public @interface ProjectionFor {
    Class<?> value();
}
