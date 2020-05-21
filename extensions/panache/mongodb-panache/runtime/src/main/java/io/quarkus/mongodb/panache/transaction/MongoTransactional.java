package io.quarkus.mongodb.panache.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * Indicate that this methods must be executed inside MongoDB transaction boundaries.
 *
 * See https://docs.mongodb.com/manual/core/transactions/
 */
@Inherited
@InterceptorBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MongoTransactional {

    /**
     * Read preference for this transaction defined as a String.
     * ReadPreference#valueOf(String) will be used to retrieve the write concern.
     * If not specified, the default from the underlying MongoClient will be used.
     */
    String readPreference() default "<default>";

    /**
     * Read concern for this transaction defined as a String.
     * ReadConcernLevel#fromString(String) will be used to retrieve the read concern.
     * If not specified, the default from the underlying MongoClient will be used.
     */
    String readConcern() default "<default>";

    /**
     * Write concern for this transaction defined as a String.
     * WriteConcern#valueOf(String) will be used to retrieve the write concern.
     * If not specified, the default from the underlying MongoClient will be used.
     */
    String writeConcern() default "<default>";
}
