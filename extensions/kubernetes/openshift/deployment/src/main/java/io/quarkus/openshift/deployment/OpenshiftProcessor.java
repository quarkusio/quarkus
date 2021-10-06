package io.quarkus.openshift.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;

public class OpenshiftProcessor {

    @BuildStep
    public void checkOpenshift(BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets) {
        deploymentTargets
                .produce(
                        new KubernetesDeploymentTargetBuildItem(OPENSHIFT, DEPLOYMENT_CONFIG, DEPLOYMENT_CONFIG_GROUP,
                                DEPLOYMENT_CONFIG_VERSION, true));
    }
}
