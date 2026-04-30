package io.quarkus.devui.spi.buildtime;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ TYPE })
@Documented
@Repeatable(DevMcpBuildTimeTools.class)
public @interface DevMcpBuildTimeTool {

    String name();

    String description();

    DevMcpParam[] params() default {};
}
