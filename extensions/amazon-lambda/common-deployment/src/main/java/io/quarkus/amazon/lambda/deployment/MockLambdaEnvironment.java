package io.quarkus.amazon.lambda.deployment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;

final class MockLambdaEnvironment {

    private MockLambdaEnvironment() {
    }

    record Values(
            String functionName,
            String functionVersion,
            String functionMemorySize,
            String logGroupName,
            String logStreamName) {
    }

    static Values fromBuildConfig(String artifactId, MockLambdaEnvironmentBuildConfig config) {
        String functionName = config.functionName()
                .orElseGet(() -> LambdaUtil.artifactToLambda(artifactId));
        String logGroupName = config.logGroupName()
                .orElse("/aws/lambda/" + functionName);
        return new Values(
                functionName,
                config.functionVersion(),
                String.valueOf(config.functionMemorySize()),
                logGroupName,
                config.logStreamName());
    }

    static Values fromBuildSystemProperties(Map<String, String> buildSystemProperties, String artifactId) {
        if (!Boolean.parseBoolean(buildSystemProperties.getOrDefault(
                "quarkus.lambda.mock-event-server.enabled", "true"))) {
            return null;
        }
        Optional<String> functionName = optionalProperty(buildSystemProperties,
                "quarkus.lambda.mock-environment.function-name");
        String resolvedFunctionName = functionName.orElseGet(() -> LambdaUtil.artifactToLambda(artifactId));
        String logGroupName = optionalProperty(buildSystemProperties,
                "quarkus.lambda.mock-environment.log-group-name")
                .orElse("/aws/lambda/" + resolvedFunctionName);
        return new Values(
                resolvedFunctionName,
                buildSystemProperties.getOrDefault("quarkus.lambda.mock-environment.function-version", "$LATEST"),
                buildSystemProperties.getOrDefault("quarkus.lambda.mock-environment.function-memory-size", "128"),
                logGroupName,
                buildSystemProperties.getOrDefault("quarkus.lambda.mock-environment.log-stream-name", "local/dev"));
    }

    static Map<String, String> toRuntimeConfig(Values values) {
        Map<String, String> config = new LinkedHashMap<>();
        config.put(AmazonLambdaApi.INTERNAL_FUNCTION_NAME, values.functionName());
        config.put(AmazonLambdaApi.INTERNAL_FUNCTION_VERSION, values.functionVersion());
        config.put(AmazonLambdaApi.INTERNAL_FUNCTION_MEMORY_SIZE, values.functionMemorySize());
        config.put(AmazonLambdaApi.INTERNAL_LOG_GROUP_NAME, values.logGroupName());
        config.put(AmazonLambdaApi.INTERNAL_LOG_STREAM_NAME, values.logStreamName());
        return config;
    }

    static Map<String, String> toEnvironmentVariables(Values values) {
        return Map.of(
                "AWS_LAMBDA_FUNCTION_NAME", values.functionName(),
                "AWS_LAMBDA_FUNCTION_VERSION", values.functionVersion(),
                "AWS_LAMBDA_FUNCTION_MEMORY_SIZE", values.functionMemorySize(),
                "AWS_LAMBDA_LOG_GROUP_NAME", values.logGroupName(),
                "AWS_LAMBDA_LOG_STREAM_NAME", values.logStreamName());
    }

    private static Optional<String> optionalProperty(Map<String, String> properties, String key) {
        return Optional.ofNullable(properties.get(key)).filter(value -> !value.isBlank());
    }
}
