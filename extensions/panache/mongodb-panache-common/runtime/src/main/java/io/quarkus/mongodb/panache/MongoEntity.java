package io.quarkus.mongodb.panache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to specify some configuration of the mapping of an entity to MongoDB.
 *
 * @deprecated use {@link io.quarkus.mongodb.panache.common.MongoEntity} instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated(forRemoval = true, since = "2.1.0")
public @interface MongoEntity {
    /**
     * The name of the collection (if not set the name of the entity class will be used)
     */
    String collection() default "";

    /**
     * The name of the database (if not set the default from the property
     * <code>quarkus.mongodb.database</code> will be used).
     */
    String database() default "";

    /**
     * The name of the MongoDB client (if not set the default client will be used).
     */
    String clientName() default "";
}
