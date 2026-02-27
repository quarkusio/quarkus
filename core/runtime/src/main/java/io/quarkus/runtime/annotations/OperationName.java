package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies an alternative name for the method part of a JsonRPC method name.
 * The full method name is composed as {@code namespace_methodName}, and this
 * annotation only overrides the {@code methodName} part while preserving the namespace.
 *
 * <p>
 * This is useful for both Dev UI and MCP (Model Context Protocol) clients.
 * Some MCP clients may introduce a name size limit,
 * so method names should ideally be under 60 characters.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * // If the namespace is "devui-continuous-testing" and the Java method is "getContinuousTestingResults",
 * // the default full name would be "devui-continuous-testing_getContinuousTestingResults" (52 chars).
 * // Using this annotation to shorten the method part:
 * &#64;OperationName("getResults")
 * &#64;JsonRpcDescription("Get the results of a Continuous testing test run")
 * public TestStatus getContinuousTestingResults() {
 *     // The effective name becomes "devui-continuous-testing_getResults" (35 chars)
 * }
 * </pre>
 */
@Retention(RUNTIME)
@Target(METHOD)
@Documented
public @interface OperationName {

    /**
     * @return the alternative name for this method
     */
    String value();

}
