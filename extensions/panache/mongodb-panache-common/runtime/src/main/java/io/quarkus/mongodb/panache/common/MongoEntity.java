package io.quarkus.mongodb.panache.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to specify some configuration of the mapping of an entity to MongoDB.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
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
