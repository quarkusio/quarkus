package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;

public class DockerStatusProcessor {

    @BuildStep
    DockerStatusBuildItem IsDockerWorking(LaunchModeBuildItem launchMode) {
        return new DockerStatusBuildItem(new IsDockerWorking(launchMode.getLaunchMode().isDevOrTest()));
    }
}
