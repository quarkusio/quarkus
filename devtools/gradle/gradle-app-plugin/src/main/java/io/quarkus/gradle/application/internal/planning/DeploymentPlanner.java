package io.quarkus.gradle.application.internal.planning;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentDescriptor;

public final class DeploymentPlanner {

    private final TaskNamePlanner taskNamePlanner = new TaskNamePlanner();

    public DeploymentPlan plan(QuarkusApplicationBuildDescriptor build,
            QuarkusApplicationDeploymentDescriptor deployment) {
        return new DeploymentPlan(deployment, taskNamePlanner.deployTaskName(build, deployment),
                deployment.imageSource(), deployment.imageReference());
    }
}
