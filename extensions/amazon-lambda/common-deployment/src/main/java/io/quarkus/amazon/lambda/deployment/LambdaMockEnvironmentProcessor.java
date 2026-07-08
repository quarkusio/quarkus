package io.quarkus.amazon.lambda.deployment;

import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

public class LambdaMockEnvironmentProcessor {

    @BuildStep(onlyIfNot = IsProduction.class)
    void configureMockLambdaEnvironment(
            LaunchModeBuildItem launchMode,
            CurateOutcomeBuildItem curateOutcome,
            LambdaBuildConfig config,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeDefaults) {
        if (!launchMode.getLaunchMode().isDevOrTest()) {
            return;
        }
        if (!config.mockEventServer().enabled()) {
            return;
        }
        if (legacyTestingEnabled()) {
            return;
        }

        String artifactId = curateOutcome.getApplicationModel().getAppArtifact().getArtifactId();
        MockLambdaEnvironment.Values values = MockLambdaEnvironment.fromBuildConfig(artifactId, config.mockEnvironment());
        MockLambdaEnvironment.toRuntimeConfig(values)
                .forEach((key, value) -> runtimeDefaults.produce(new RunTimeConfigurationDefaultBuildItem(key, value, false)));
    }

    private boolean legacyTestingEnabled() {
        try {
            Class.forName("io.quarkus.amazon.lambda.test.LambdaClient");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
