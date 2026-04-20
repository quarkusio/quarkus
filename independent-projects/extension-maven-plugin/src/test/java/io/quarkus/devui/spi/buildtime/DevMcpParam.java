package io.quarkus.devui.spi.buildtime;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

@Retention(RUNTIME)
@Documented
public @interface DevMcpParam {

    String name();

    String description() default "";

    boolean required() default true;
}
