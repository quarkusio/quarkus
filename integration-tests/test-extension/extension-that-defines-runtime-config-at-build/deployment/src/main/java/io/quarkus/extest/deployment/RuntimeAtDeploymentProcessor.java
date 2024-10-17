package io.quarkus.extest.deployment;

import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class RuntimeAtDeploymentProcessor {

    @BuildStep
    public void buildStep(RuntimeAtDeploymentConfig config, BuildProducer<ConfigPropertyBuildItem> props) {

    }
}
