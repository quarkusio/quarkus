package io.quarkus.amazon.lambda.deployment;

import java.util.Collections;
import java.util.Map;

import io.quarkus.deployment.dev.DevModeLauncherEnvironmentProvider;

public class LambdaDevModeLauncherEnvironmentProvider implements DevModeLauncherEnvironmentProvider {

    @Override
    public Map<String, String> provide(Map<String, String> buildSystemProperties, String applicationName) {
        MockLambdaEnvironment.Values values = MockLambdaEnvironment.fromBuildSystemProperties(buildSystemProperties,
                applicationName);
        if (values == null) {
            return Collections.emptyMap();
        }
        return MockLambdaEnvironment.toEnvironmentVariables(values);
    }
}
