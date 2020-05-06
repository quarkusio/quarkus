package io.quarkus.minikube.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;
import static io.quarkus.kubernetes.deployment.Constants.MINIKUBE;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;

public class MinikubeProcessor {

    @BuildStep
    public void checkOpenshift(BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets) {
        deploymentTargets
                .produce(new KubernetesDeploymentTargetBuildItem(MINIKUBE, DEPLOYMENT, true));
    }
}
