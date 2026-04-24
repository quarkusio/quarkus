package io.quarkus.devui.spi.buildtime;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

/**
 * Describes a parameter of a build-time MCP tool declared via {@link DevMcpBuildTimeTool}.
 */
@Retention(RUNTIME)
@Documented
public @interface DevMcpParam {

    /**
     * The parameter name.
     */
    String name();

    /**
     * A human-readable description of this parameter.
     */
    String description() default "";

    /**
     * Whether this parameter is required. Defaults to {@code true}.
     */
    boolean required() default true;
}
