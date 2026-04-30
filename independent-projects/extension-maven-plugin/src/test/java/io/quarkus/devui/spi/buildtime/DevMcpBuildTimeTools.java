package io.quarkus.devui.spi.buildtime;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ TYPE })
@Documented
public @interface DevMcpBuildTimeTools {

    DevMcpBuildTimeTool[] value();
}
