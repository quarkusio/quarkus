package io.quarkus.kubernetes.client.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAllowIncompleteClasspathBuildItem;

public class NativeOverrides {

    @BuildStep
    NativeImageAllowIncompleteClasspathBuildItem incompleteModel() {
        return new NativeImageAllowIncompleteClasspathBuildItem("quarkus-kubernetes-client");
    }
}
