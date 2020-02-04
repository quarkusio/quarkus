package io.quarkus.mongodb.panache;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation can be used to specify some configuration of the mapping of an entity to MongoDB.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MongoEntity {
    /**
     * The name of the collection (if not set the name of the entity class will be used)
     */
    String collection() default "";

    /**
     * the name of the database (if not set the default from the property
     * <code>quarkus.mongodb.database</code> will be used.
     */
    String database() default "";
}
