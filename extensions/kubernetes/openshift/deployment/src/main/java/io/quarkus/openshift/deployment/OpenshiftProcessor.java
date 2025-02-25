package io.quarkus.openshift.deployment;

import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.kubernetes.deployment.DeploymentResourceKind;
import io.quarkus.kubernetes.deployment.OpenShiftConfig;
import io.quarkus.kubernetes.deployment.ResourceNameUtil;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;

public class OpenshiftProcessor {

    @BuildStep
    public void checkOpenshift(ApplicationInfoBuildItem applicationInfo, Capabilities capabilities, OpenShiftConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {

        DeploymentResourceKind deploymentResourceKind = config.getDeploymentResourceKind(capabilities);
        deploymentTargets
                .produce(
                        new KubernetesDeploymentTargetBuildItem(OPENSHIFT, deploymentResourceKind.getKind(),
                                deploymentResourceKind.getGroup(),
                                deploymentResourceKind.getVersion(), true,
                                config.deployStrategy()));

        String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        resourceMeta.produce(new KubernetesResourceMetadataBuildItem(OPENSHIFT, deploymentResourceKind.getGroup(),
                deploymentResourceKind.getVersion(), deploymentResourceKind.getKind(), name));
    }
}
