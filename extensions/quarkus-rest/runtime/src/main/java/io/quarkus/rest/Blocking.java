package io.quarkus.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//TODO: there should just be one Quarkus blocking annotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Blocking {

    /**
     * true if this endpoint is blocking.
     */
    boolean value() default true;
}
