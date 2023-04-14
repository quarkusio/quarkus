package io.quarkus.observability.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DevTarget {
    /**
     * The dev resource we require on the classpath,
     * for this config to fully kick-in.
     */
    String value();
}
