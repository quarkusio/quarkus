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

    /**
     * Set to true to delegate to the super method instead of JpaOperations. This is useful to
     * still inject interceptor calls and mock stubs.
     */
    boolean callSuperMethod() default false;

    /**
     * Set to false when the implemented method should not receive the entity type as one of its parameters
     */
    boolean ignoreEntityTypeParam() default false;
}
