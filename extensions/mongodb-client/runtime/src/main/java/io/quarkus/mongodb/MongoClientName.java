package io.quarkus.mongodb;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Marker annotation to select mongo connection of cluster configuration
 * Use name parameter to select it
 *
 * For example, if a mongo connection is configured like so in {@code application.properties}:
 *
 * <pre>
 * quarkus.mongodb.cluster1.connection-string=mongodb://localhost:27018
 * </pre>
 *
 * Then to inject the proper {@code MongoClient}, you would need to use {@code MongoClientName} like so:
 *
 * <pre>
 *     &#64Inject
 *     &#64MongoClientName("cluster1")
 *     MongoClient client;
 * </pre>
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
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
