package io.quarkus.panache.common.impl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GenerateBridge {

    /**
     * Set to true if the corresponding JpaOperations method returns Object
     * but the bridge should return a more specific type.
     */
    boolean targetReturnTypeErased() default false;

}
