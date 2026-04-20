package io.quarkus.devui.spi.buildtime;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Declares a build-time MCP tool that is enabled by default. Place this annotation
 * on a {@code @BuildStep} processor class to make the tool discoverable via Jandex
 * scanning at build time.
 * <p>
 * This annotation is the deployment-time counterpart of
 * {@link io.quarkus.runtime.annotations.JsonRpcDescription} +
 * {@link io.quarkus.runtime.annotations.DevMCPEnableByDefault} used on runtime JSON-RPC methods.
 * It allows the {@code aggregate-skills} Maven goal to discover build-time MCP tools without
 * parsing source code.
 *
 * <pre>
 * &#64;DevMcpBuildTimeTool(name = "runTests", description = "Run all tests", params = {
 *         &#64;DevMcpParam(name = "className", description = "Test class name", required = false)
 * })
 * public class TestingProcessor {
 *     // ...
 * }
 * </pre>
 */
@Retention(RUNTIME)
@Target({ TYPE })
@Documented
@Repeatable(DevMcpBuildTimeTools.class)
public @interface DevMcpBuildTimeTool {

    /**
     * The tool name, matching the {@code methodName} in the {@code BuildTimeActionBuildItem} builder chain.
     */
    String name();

    /**
     * A human-readable description of what this tool does.
     */
    String description();

    /**
     * The parameters this tool accepts. Defaults to no parameters.
     */
    DevMcpParam[] params() default {};
}
