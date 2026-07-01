package io.quarkus.devmcp.spi;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

@Retention(RUNTIME)
public @interface DevMcpParam {

    String name();

    String description() default "";

    boolean required() default true;
}
