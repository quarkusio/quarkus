package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.PodmanStatusBuildItem;

public class PodmanStatusProcessor {
    @BuildStep
    PodmanStatusBuildItem isPodmanWorking(LaunchModeBuildItem launchMode) {
        return new PodmanStatusBuildItem(new IsPodmanWorking(launchMode.getLaunchMode().isDevOrTest()));
    }
}
