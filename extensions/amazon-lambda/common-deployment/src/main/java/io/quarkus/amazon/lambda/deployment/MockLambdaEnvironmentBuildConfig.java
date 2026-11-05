package io.quarkus.amazon.lambda.deployment;

import java.util.Optional;

import io.smallrye.config.WithDefault;

/**
 * Configuration for mock AWS Lambda environment variables in dev and test mode.
 */
public interface MockLambdaEnvironmentBuildConfig {

    /**
     * Mock function name. Defaults to the application artifact id transformed to a Lambda-compatible name.
     */
    Optional<String> functionName();

    /**
     * Mock function version.
     */
    @WithDefault("$LATEST")
    String functionVersion();

    /**
     * Mock function memory size in MB.
     */
    @WithDefault("128")
    int functionMemorySize();

    /**
     * Mock CloudWatch log group name. Defaults to {@code /aws/lambda/<function-name>}.
     */
    Optional<String> logGroupName();

    /**
     * Mock CloudWatch log stream name.
     */
    @WithDefault("local/dev")
    String logStreamName();
}
