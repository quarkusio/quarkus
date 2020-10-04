package io.quarkus.openshift.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;

import io.quarkus.container.image.openshift.deployment.OpenshiftBuild;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;

public class OpenshiftProcessor {

    @BuildStep(onlyIf = OpenshiftBuild.class)
    public CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.CONTAINER_IMAGE_OPENSHIFT);
    }

    @BuildStep
    public void checkOpenshift(BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets) {
        deploymentTargets
                .produce(new KubernetesDeploymentTargetBuildItem(OPENSHIFT, DEPLOYMENT_CONFIG, true));
    }
}
