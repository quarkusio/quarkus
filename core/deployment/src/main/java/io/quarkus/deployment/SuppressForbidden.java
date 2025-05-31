package io.quarkus.deployment;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to suppress forbidden-apis errors inside for the element it's added to.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface SuppressForbidden {

    @AliasFor("value")
    String reason() default "";

    @AliasFor("reason")
    String value() default "";
}