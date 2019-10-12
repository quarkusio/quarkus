package io.quarkus.mongodb.runtime;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Marker annotation to select mongo connection of cluster configuration
 * Use name parameter to select it
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface MongoClientName {
    /**
     * Specify the cluster name of the connection.
     *
     * @return the value
     */
    String value() default "";
}
