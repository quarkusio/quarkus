package io.quarkus.resteasy.reactive.links;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RestLink {

    String rel() default "";

    Class<?> entityType() default Object.class;
}
